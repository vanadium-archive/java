// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import io.v.rx.RxVIterable;
import io.v.rx.VFn;
import io.v.v23.VIterable;
import io.v.v23.context.CancelableVContext;
import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.nosql.BatchOptions;
import io.v.v23.services.watch.ResumeMarker;
import io.v.v23.syncbase.nosql.BatchDatabase;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.DatabaseCore;
import io.v.v23.syncbase.nosql.Table;
import io.v.v23.syncbase.nosql.WatchChange;
import io.v.v23.verror.NoExistException;
import io.v.v23.verror.VException;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

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

        mObservable = rxDb.getObservable().map(VFn.unchecked(this::mapFrom));
    }

    @Override
    public Table mapFrom(final DatabaseCore db) throws VException {
        final Table t = db.getTable(mName);
        SyncbaseEntity.compose(t::exists, t::create).ensureExists(mVContext, null);
        return t;
    }

    private <T> T getInitial(final BatchDatabase db, final String tableName, final String key,
                             final Class<T> type, final T defaultValue) throws VException {
        try {
            @SuppressWarnings("unchecked")
            final T fromGet = (T) db.getTable(tableName).get(
                    mVContext, key, type);
            return fromGet;
        } catch (final NoExistException e) {
            return defaultValue;
        }
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
                                    final T defaultValue) throws VException {
        /*
        Watch will not work properly unless the table exists (sync will not create the table),
        and table creation must happen outside the batch.
        https://github.com/vanadium/issues/issues/857
        */
        mapFrom(db);

        final BatchDatabase batch = db.beginBatch(mVContext, new BatchOptions("", true));
        final T initial = getInitial(batch, mName, key, type, defaultValue);
        final ResumeMarker r = batch.getResumeMarker(mVContext);
        subscriber.onNext(new WatchEvent<>(initial, r, false));
        batch.abort(mVContext);

        final CancelableVContext cancelable = mVContext.withCancel();
        final VIterable<WatchChange> s = db.watch(cancelable, mName, key, r);
        log.debug("Watching {}: {}", mName, key);
        cancelContextOnDisconnect(subscriber, cancelable, key);
        observeWatchStream(s, key, defaultValue).subscribe(subscriber);
    }

    /**
     * Watches a specific Syncbase row for changes.
     *
     * TODO(rosswang): Cache this by args.
     */
    public <T> Observable<WatchEvent<T>> watch(final String key, final Class<T> type,
                                               final T defaultValue) {
        return Observable.<WatchEvent<T>>create(subscriber -> mRxDb.getObservable()
                .observeOn(Schedulers.io())
                .subscribe(
                        VFn.unchecked(db -> {
                            /*
                            Could be an expression lambda, but that confuses both RetroLambda and
                            AndroidStudio.
                            */
                            subscribeWatch(subscriber, db, key, type, defaultValue);
                        }),
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
