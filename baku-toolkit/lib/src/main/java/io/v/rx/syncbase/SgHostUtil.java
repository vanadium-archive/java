// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import org.joda.time.Duration;

import io.v.rx.MountEvent;
import io.v.rx.RxNamespace;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Server;
import io.v.v23.syncbase.Syncbase;
import io.v.v23.verror.TimeoutException;
import io.v.v23.verror.VException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * This utility class is a short-term solution until a better solution for distributed syncgroup
 * hosting is available.
 */
@Slf4j
@UtilityClass
public class SgHostUtil {
    public static final Duration SYNCBASE_PING_TIMEOUT = Duration.standardSeconds(5);

    /**
     * This method blocks while it pings Syncbase at the given name.
     *
     * @throws InterruptedException if the thread is interrupted while waiting for the ping
     *                              response. The default implementation does not throw this.
     *                              TODO(rosswang): pick this out from the VException, if possible.
     */
    public static boolean isSyncbaseOnline(final VContext vContext, final String name)
            throws InterruptedException {
        final VContext pingContext = vContext.withTimeout(SYNCBASE_PING_TIMEOUT);
        try {
            /* It would be nice if there were a more straightforward ping. We can't just query the
            mount table because the server might not have shut down cleanly. */
            Syncbase.newService(name).getApp("ping").exists(pingContext);
            return true;
        } catch (final TimeoutException e) {
            return false;
        } catch (final VException e) {
            log.error("Unexpected error while attempting to ping Syncgroup host at " + name, e);
            return false;
        }
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
        return rxServer.observeOn(Schedulers.io())
        .switchMap(s -> {
            try {
                if (isSyncbaseOnline(vContext, name)) {
                    return Observable.just(0);
                } else {
                    return mountSgHost(rxServer, name);
                }
            } catch (final InterruptedException e) {
                return Observable.error(e);
            }
        });
    }
}
