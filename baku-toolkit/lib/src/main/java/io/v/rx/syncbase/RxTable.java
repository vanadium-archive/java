// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import javax.annotation.CheckReturnValue;

import io.v.rx.RxInputChannel;
import io.v.rx.VFn;
import io.v.v23.InputChannel;
import io.v.v23.context.CancelableVContext;
import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.nosql.BatchOptions;
import io.v.v23.services.syncbase.nosql.KeyValue;
import io.v.v23.services.watch.ResumeMarker;
import io.v.v23.syncbase.nosql.BatchDatabase;
import io.v.v23.syncbase.nosql.ChangeType;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.DatabaseCore;
import io.v.v23.syncbase.nosql.PrefixRange;
import io.v.v23.syncbase.nosql.RowRange;
import io.v.v23.syncbase.nosql.Table;
import io.v.v23.syncbase.nosql.WatchChange;
import io.v.v23.verror.NoExistException;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action2;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.ReplaySubject;
import rx.subscriptions.Subscriptions;

import static net.javacrumbs.futureconverter.guavarx.FutureConverter.toObservable;

@Accessors(prefix = "m")
@Getter
@Slf4j
public class RxTable extends RxEntity<Table, DatabaseCore> {
    @AllArgsConstructor
    private static class InitialArtifacts<T> {
        public final T initial;
        public final ResumeMarker resumeMarker;
    }

    @Value
    public static class Row<T> {
        String mRowName;
        T mValue;
    }

    private final VContext mVContext;
    private final String mName;
    private final RxDb mRxDb;

    private final Observable<Table> mObservable;

    public RxTable(final String name, final RxDb rxDb) {
        mVContext = rxDb.getVContext();
        mName = name;
        mRxDb = rxDb;

        mObservable = rxDb.getObservable().switchMap(this::mapFrom);
    }

    @Override
    public Observable<Table> mapFrom(final DatabaseCore db) {
        final Table t = db.getTable(mName);
        return toObservable(SyncbaseEntity.compose(t::exists, t::create)
                .ensureExists(mVContext, null))
                .map(x -> t);
    }

    private <T> ListenableFuture<T> getInitial(
            final BatchDatabase db, final String tableName, final String key, final Class<T> type,
            final T defaultValue) {
        @SuppressWarnings("unchecked")
        final ListenableFuture<T> fromGet = (ListenableFuture<T>) db.getTable(tableName).get(
                mVContext, key, type);
        return Futures.withFallback(fromGet, t -> t instanceof NoExistException ?
                Futures.immediateFuture(defaultValue) : Futures.immediateFailedFuture(t));
    }

    @SuppressWarnings("unchecked")
    private <T> Observable<Row<T>> getInitial(
            final BatchDatabase db, final String tableName, final RowRange keys,
            @Nullable final Func1<String, Boolean> keyFilter, final Class<T> type) {
        Observable<KeyValue> untyped = RxInputChannel.wrap(
                db.getTable(tableName).scan(mVContext, keys)).autoConnect();

        if (keyFilter != null) {
            untyped = untyped.filter(kv -> keyFilter.call(kv.getKey()));
        }
        return untyped.concatMap(VFn.wrap(kv -> new Row<>(kv.getKey(),
                (T) VomUtil.decode(kv.getValue(), type))));
    }

    /**
     * Wraps a prefix watch stream in a key-specific observable. It remains to be seen whether it
     * will be better to feature-request an exact-match watch API from Syncbase or consolidate all
     * watches into one stream. Exact-match presents a cleaner API boundary but results in more
     * underlying streams, whereas consolidating at the library level will usually be more efficient
     * unless large portions of data won't need to be watched, and also it opens up questions of
     * whether we should computationally optimize the prefix query.
     *
     * @return an observable wrapping the watch stream. This observable should only be subscribed to
     * once, as we can only iterate over the underlying stream once.
     */
    private static <T> Observable<SingleWatchEvent<T>> observeWatchStream(
            final InputChannel<WatchChange> s, final String key, final T defaultValue) {
        return RxInputChannel.wrap(s)
                .autoConnect()
                .filter(c -> c.getRowName().equals(key))
                        // About the Vfn.wrap, on error, the wrapping replay will disconnect,
                        // calling cancellation (see cancelOnDisconnect). The possible source of
                        // VException here is VOM decoding.
                .concatMap(VFn.wrap(c -> SingleWatchEvent.fromWatchChange(c, defaultValue)))
                .distinctUntilChanged();
    }

    private static class RangeWatchBatchWindower<T> {
        private final Subscriber<? super RangeWatchBatch<T>> mSubscriber;

        private ReplaySubject<RangeWatchEvent<T>> mSub;

        private void ensureBatch(final ResumeMarker resumeMarker) {
            if (mSub == null) {
                mSub = ReplaySubject.create();
                mSubscriber.onNext(new RangeWatchBatch<>(resumeMarker, mSub));
            }
        }

        public RangeWatchBatchWindower(final Subscriber<? super RangeWatchBatch<T>> subscriber) {
            mSubscriber = subscriber;
            mSubscriber.add(Subscriptions.create(this::onBatchEnd));
        }

        public void onNext(final ResumeMarker resumeMarker, final RangeWatchEvent<T> change) {
            ensureBatch(resumeMarker);
            mSub.onNext(change);
        }

        public void onError(final ResumeMarker resumeMarker, final Throwable t) {
            ensureBatch(resumeMarker);
            mSub.onError(t);
            mSub = null;
        }

        public void onBatchEnd() {
            if (mSub != null) {
                mSub.onCompleted();
                mSub = null;
            }
        }
    }

    /**
     * Wraps a watch stream in an observable.
     *
     * @return an observable wrapping the watch stream, grouped by batches. These observables should
     * only be subscribed to once, as we can only iterate over the underlying stream once.
     */
    private static <T> Observable<RangeWatchBatch<T>> observeWatchStream(
            final InputChannel<WatchChange> s, @Nullable final Func1<String, Boolean> prefixFilter,
            final Class<T> type) {
        // TODO(rosswang): support other RowRange types
        final Observable<WatchChange> raw = RxInputChannel.wrap(s).autoConnect();

        return Observable.create(subscriber -> {
                    final RangeWatchBatchWindower<T> windower =
                            new RangeWatchBatchWindower<T>(subscriber);

                    subscriber.add(raw.subscribe(c -> {
                                if (prefixFilter == null || prefixFilter.call(c.getRowName())) {
                                    try {
                                        windower.onNext(c.getResumeMarker(),
                                                RangeWatchEvent.fromWatchChange(c, type));
                                    } catch (final VException e) {
                                        windower.onError(c.getResumeMarker(), e);
                                    }
                                }
                                if (!c.isContinued()) {
                                    windower.onBatchEnd();
                                }
                            },
                            subscriber::onError,
                            subscriber::onCompleted
                    ));
                }
        );
    }

    private void cancelContextOnDisconnect(final Subscriber<?> subscriber,
                                           final CancelableVContext cancelable,
                                           final String prefix) {
        subscriber.add(Subscriptions.create(() -> {
            log.debug("Cancelling watch on {}: {}", mName, prefix);
            cancelable.cancel();
        }));
    }

    private <T, I, C> void subscribeWatch(
            final Subscriber<T> subscriber, final Database db,
            final String prefix, final Func1<BatchDatabase, ListenableFuture<I>> getInitial,
            final Func1<InputChannel<WatchChange>, Observable<C>> observeWatchStream,
            final Func2<InitialArtifacts<I>, Observable<C>, Observable<? extends T>> mergeInitial) {
        // Watch will not work properly unless the table exists (sync will not create the table),
        // and table creation must happen outside the batch.
        // https://github.com/vanadium/issues/issues/857
        mapFrom(db)
                .switchMap(t -> toObservable(db.beginBatch(mVContext, new BatchOptions("", true))))
                .switchMap(batch -> Observable.<I, ResumeMarker, InitialArtifacts<I>>combineLatest(
                        toObservable(getInitial.call(batch)),
                        toObservable(batch.getResumeMarker(mVContext)),
                        InitialArtifacts::new)
                        .doOnTerminate(() -> toObservable(batch.abort(mVContext))
                                .subscribe(v -> {
                                }, t -> log.warn("Unable to abort watch initial read query", t))))
                .switchMap(i -> {
                    final CancelableVContext cancelable = mVContext.withCancel();
                    cancelContextOnDisconnect(subscriber, cancelable, prefix);
                    log.debug("Watching {}: {}", mName, prefix);
                    return mergeInitial.call(i, observeWatchStream.call(
                            db.watch(cancelable, mName, prefix, i.resumeMarker)));
                }).subscribe(subscriber::onNext, subscriber::onError); // Don't connect onComplete
    }

    private <T> void subscribeWatch(final Subscriber<? super SingleWatchEvent<T>> subscriber,
                                    final Database db, final String key, final Class<T> type,
                                    final T defaultValue) {
        subscribeWatch(subscriber, db, key,
                b -> getInitial(b, mName, key, type, defaultValue),
                s -> observeWatchStream(s, key, defaultValue),
                (i, s) -> s.startWith(new SingleWatchEvent<>(i.initial, i.resumeMarker, false)));
    }

    private <T> void subscribeWatch(
            final Subscriber<? super RangeWatchBatch<T>> subscriber, final Database db,
            final PrefixRange prefix, @Nullable final Func1<String, Boolean> keyFilter,
            final Class<T> type) {
        subscribeWatch(subscriber, db, prefix.getPrefix(),
                b -> Futures.immediateFuture(getInitial(b, mName, prefix, keyFilter, type)),
                s -> RxTable.observeWatchStream(s, keyFilter, type),
                (i, s) -> s.startWith(new RangeWatchBatch<>(i.resumeMarker, i.initial.map(r ->
                        new RangeWatchEvent<>(r, ChangeType.PUT_CHANGE, false)))));
    }

    // TODO(rosswang): Cache this by args.
    // TODO(rosswang): Possibly unsubscribe previous watch on mRxDb onNext.
    private <T> Observable<T> watch(final Action2<Database, Subscriber<? super T>> subscribeWatch) {
        return Observable.<T>create(s -> mRxDb.getObservable()
                //onComplete is connected by subscribeWatch/observeWatchStream.subscribe
                .subscribe(db -> subscribeWatch.call(db, s), s::onError))
                // Don't create new watch streams for subsequent subscribers, but do cancel the
                // stream if no subscribers are listening (and restart if new subscriptions happen).
                .replay(1)
                .refCount();
    }

    /**
     * Watches a specific Syncbase row for changes.
     */
    public <T> Observable<SingleWatchEvent<T>> watch(final String key, final Class<T> type,
                                                     final T defaultValue) {
        return watch((db, s) -> subscribeWatch(s, db, key, type, defaultValue));
    }

    /**
     * Watches a Syncbase prefix for changes.
     */
    public <T> Observable<RangeWatchBatch<T>> watch(
            final PrefixRange prefix, @Nullable final Func1<String, Boolean> keyFilter,
            final Class<T> type) {
        return watch((db, s) -> subscribeWatch(s, db, prefix, keyFilter, type));
    }

    @CheckReturnValue
    public <T> Observable<T> exec(final Func1<Table, ListenableFuture<T>> op) {
        return once()
                .flatMap(t -> toObservable(op.call(t)))
                .replay().autoConnect();
    }

    public <T> Observable<Void> put(final String key, final T value,
                                    final Class<T> type) {
        return exec(t -> t.put(mVContext, key, value, type));
    }

    @SuppressWarnings("unchecked")
    public <T> Observable<Void> put(final String key, @NonNull final T value) {
        return put(key, value, (Class<T>) value.getClass());
    }

    @SuppressWarnings("unchecked")
    public <T> Observable<T> get(final String key, final Class<? extends T> type) {
        return exec(t -> t.get(mVContext, key, type))
                .map(x -> (T) x);
    }

    public <T> Observable<T> get(final String key, final Class<? extends T> type,
                                 final T defaultValue) {
        return get(key, type).onErrorResumeNext(t -> t instanceof NoExistException ?
                Observable.just(defaultValue) : Observable.error(t));
    }

    public Observable<Void> delete(final String key) {
        return exec(t -> t.delete(mVContext, key));
    }

    public Observable<Void> destroy() {
        return exec(t -> t.destroy(mVContext));
    }
}
