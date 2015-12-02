// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import io.v.rx.RxVIterable;
import io.v.rx.VFn;
import io.v.v23.VIterable;
import io.v.v23.context.CancelableVContext;
import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.nosql.BatchOptions;
import io.v.v23.syncbase.nosql.BatchDatabase;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.DatabaseCore;
import io.v.v23.syncbase.nosql.Table;
import io.v.v23.syncbase.nosql.WatchChange;
import io.v.v23.verror.NoExistException;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Subscriber;
import rx.subscriptions.Subscriptions;

import static net.javacrumbs.futureconverter.guavarx.FutureConverter.toObservable;

@Accessors(prefix = "m")
@Getter
@Slf4j
public class RxTable extends RxEntity<Table, DatabaseCore> {
    private final VContext mVContext;
    private final String mName;
    private final RxDb mRxDb;

    private final Observable<Table> mObservable;

    public RxTable(final String name, final RxDb rxDb) {
        mVContext = rxDb.getVContext();
        mName = name;
        mRxDb = rxDb;

        mObservable = rxDb.getObservable().flatMap(this::mapFrom);
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
    private static <T> Observable<WatchEvent<T>> observeWatchStream(
            final VIterable<WatchChange> s, final String key, final T defaultValue) {
        return RxVIterable.wrap(s)
                .filter(c -> c.getRowName().equals(key))
                /*
                About the Vfn.unchecked, on error, the wrapping replay will disconnect, calling
                cancellation (see cancelOnDisconnect). The possible source of VException here is VOM
                decoding.
                 */
                .map(VFn.unchecked(c -> {
                    return WatchEvent.fromWatchChange(c, defaultValue);
                }))
                .distinctUntilChanged();
    }

    private void cancelContextOnDisconnect(final Subscriber<?> subscriber,
                                           final CancelableVContext cancelable,
                                           final String key) {
        subscriber.add(Subscriptions.create(() -> {
            log.debug("Cancelling watch on {}: {}", mName, key);
            cancelable.cancel();
        }));
    }

    private <T> void subscribeWatch(final Subscriber<? super WatchEvent<T>> subscriber,
                                    final Database db, final String key, final Class<T> type,
                                    final T defaultValue) {
        /*
        Watch will not work properly unless the table exists (sync will not create the table),
        and table creation must happen outside the batch.
        https://github.com/vanadium/issues/issues/857
        */
        mapFrom(db)
                .flatMap(t -> toObservable(db.beginBatch(mVContext, new BatchOptions("", true))))
                .flatMap(batch -> Observable.combineLatest(
                        toObservable(getInitial(batch, mName, key, type, defaultValue)),
                        toObservable(batch.getResumeMarker(mVContext)),
                        (initial, r) -> new WatchEvent<>(initial, r, false))
                        .doOnTerminate(() -> toObservable(batch.abort(mVContext))
                                .subscribe(v -> {
                                }, t -> log.warn("Unable to abort watch initial read query", t))))
                .flatMap(e -> {
                    final CancelableVContext cancelable = mVContext.withCancel();
                    cancelContextOnDisconnect(subscriber, cancelable, key);
                    return toObservable(db.watch(cancelable, mName, key, e.getResumeMarker()))
                            .doOnNext(s -> log.debug("Watching {}: {}", mName, key))
                            .flatMap(s -> observeWatchStream(s, key, defaultValue))
                            .startWith(e);
                }).subscribe(subscriber);
    }

    /**
     * Watches a specific Syncbase row for changes.
     * <p>
     * TODO(rosswang): Cache this by args.
     */
    public <T> Observable<WatchEvent<T>> watch(final String key, final Class<T> type,
                                               final T defaultValue) {
        return Observable.<WatchEvent<T>>create(subscriber -> mRxDb.getObservable()
                .subscribe(
                        db -> subscribeWatch(subscriber, db, key, type, defaultValue),
                        subscriber::onError
                        //onComplete is connected by subscribeWatch/observeWatchStream.subscribe
                ))
                /*
                Don't create new watch streams for subsequent subscribers, but do cancel the stream
                if no subscribers are listening (and restart if new subscriptions happen).
                 */
                .replay(1)
                .refCount();
    }
}
