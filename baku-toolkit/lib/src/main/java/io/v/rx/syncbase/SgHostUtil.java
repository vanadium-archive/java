// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import com.google.common.collect.Iterables;

import org.joda.time.Duration;

import io.v.rx.MountEvent;
import io.v.rx.RxNamespace;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Server;
import io.v.v23.syncbase.Syncbase;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.verror.TimeoutException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;

import static net.javacrumbs.futureconverter.guavarx.FutureConverter.toObservable;

/**
 * This utility class is a short-term solution until a better solution for distributed syncgroup
 * hosting is available.
 */
@Slf4j
@UtilityClass
public class SgHostUtil {
    public static final Duration SYNCBASE_PING_TIMEOUT = Duration.standardSeconds(5);

    /**
     * @return an observable that emits a single boolean indicating whether a Syncbase instance
     * mounted at {@code name} is responsive within {@link #SYNCBASE_PING_TIMEOUT}.
     */
    public static Observable<Boolean> isSyncbaseOnline(final VContext vContext, final String name) {
        final VContext pingContext = vContext.withTimeout(SYNCBASE_PING_TIMEOUT);
        /*
        It would be nice if there were a more straightforward ping. We can't just query the mount
        table because the server might not have shut down cleanly.
        TODO(rosswang): I think sadovsky@ has added this, but it doesn't appear exposed yet.
        */
        return toObservable(Syncbase.newService(name).getApp("ping").exists(pingContext))
                .map(e -> true)
                .onErrorResumeNext(t -> t instanceof TimeoutException ?
                        Observable.just(false) : Observable.error(t));
    }

    /**
     * @return {@code true} iff the mount event represents a successful mount.
     */
    private static boolean processMountEvent(final MountEvent e) {
        e.getServer().ifPresentOrElse(
                s -> e.getError().ifPresentOrElse(
                        err -> log.error(String.format("Could not %s local Syncbase instance %s " +
                                        "as syncgroup host %s",
                                e.isMount() ? "mount" : "unmount", s, e.getName()), err),
                        () -> log.info("{} local Syncbase instance {} as syncgroup host {}",
                                e.isMount() ? "Mounted" : "Unmounted", s, e.getName())
                ),
                () -> e.getError().ifPresentOrElse(
                        err -> log.error("Could not mount local Syncbase instance as syncgroup " +
                                "host " + e.getName(), err),
                        () -> log.info("Mounting local Syncbase instance as syncgroup host " +
                                e.getName()))
        );
        return e.isSuccessfulMount();
    }

    private static Observable<MountEvent> mountSgHost(final Observable<Server> rxServer,
                                                      final String name) {
        return RxNamespace.mount(rxServer, name)
                .filter(SgHostUtil::processMountEvent)
                .retry((i, t) -> {
                    log.error("Error maintaining mount of local Syncbase instance as " +
                            "syncgroup host " + name, t);
                    return t instanceof Exception;
                })
                .replay().refCount();
    }

    /**
     * @return an observable that emits an item when a Syncbase instance is known to be hosted at
     * the given name. The mount is updated for any new server instances until this observable has
     * been unsubscribed.
     */
    public static Observable<Object> ensureSyncgroupHost(
            final VContext vContext, final Observable<Server> rxServer, final String name) {
        return rxServer.switchMap(s -> isSyncbaseOnline(vContext, name)
                .flatMap(online -> online ? Observable.just(0) : mountSgHost(rxServer, name)));
    }

    /**
     * @return an observable that echos the db after the each db emitted by
     * {@link RxDb#getObservable()} has been ensured to possess the given table names. Upon
     * subscription, for each db emitted, the observable will create these app/db/table hierarchies
     * if not already present.
     */
    public static Observable<Database> ensureSyncgroupHierarchies(
            final RxDb rxDb, final Iterable<String> tableNames) {
        return rxDb.getObservable().switchMap(db -> Observable.merge(Iterables.transform(tableNames,
                t -> rxDb.rxTable(t)
                        .mapFrom(db)
                        .map(rxt -> db)))
                .lastOrDefault(db));
    }
}
