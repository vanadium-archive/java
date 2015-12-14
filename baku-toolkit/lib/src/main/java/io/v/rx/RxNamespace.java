// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.v.v23.rpc.MountStatus;
import io.v.v23.rpc.MountStatusKey;
import io.v.v23.rpc.MountStatusValue;
import io.v.v23.rpc.Server;
import io.v.v23.verror.VException;
import java8.util.Maps;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import rx.Observable;

@UtilityClass
public class RxNamespace {
    @RequiredArgsConstructor
    @AllArgsConstructor
    private static class MountRecord {
        /*
        TODO(rosswang): https://github.com/vanadium/issues/issues/826
        Right now, mount status defaults timestamps for events that have never happened to be some
        time in 1754. As a hack, initialize these to epoch/1970 (or any time after the default).
         */
        public DateTime lastMount = new DateTime(0),
                lastUnmount = new DateTime(0);

        public static MountRecord fromStatus(final MountStatus status) {
            return new MountRecord(status.getLastMount(), status.getLastUnmount());
        }
    }

    private static Observable<MountEvent> processStatus(
            final Map<MountStatusKey, MountRecord> mountRecords,
            final MountStatusKey k, final MountStatusValue v) {

        final List<MountEvent> newEvents = new ArrayList<>(2);
        synchronized (mountRecords) {
            final MountRecord record = Maps.computeIfAbsent(mountRecords, k,
                    kk -> new MountRecord());
            if (v.getLastMount() != null && v.getLastMount().isAfter(record.lastMount)) {
                newEvents.add(MountEvent.forStatus(true, k.getName(), k.getServer(),
                        v.getLastMount(), v.getLastMountError()));
            }
            if (v.getLastUnmount() != null && v.getLastUnmount().isAfter(record.lastUnmount)) {
                newEvents.add(MountEvent.forStatus(false, k.getName(), k.getServer(),
                        v.getLastUnmount(), v.getLastUnmountError()));
            }
            record.lastMount = v.getLastMount();
            record.lastUnmount = v.getLastUnmount();
        }
        Collections.sort(newEvents, MountEvent::compareByTimestamp);
        return Observable.from(newEvents);
    }

    private static boolean isAlreadyMounted(final MountStatus status) {
        //There are also ambiguous cases; err on the side of false.
        return status.getLastMount().isAfter(status.getLastUnmount()) &&
                status.getLastMountError() == null;
    }

    /**
     * @return an {@code Observable} of {@code MountEvent}s. Events including servers come from
     * polling and may or may not include an error. Events without servers are from
     * {@link Server#addName(String)} and may or may not include an error. If a server is already
     * mounted, a backdated mount event is included. The observable completes if and when the server
     * is unmounted.
     */
    public static Observable<MountEvent> mount(final Observable<Server> rxServer,
                                                          final String name) {
        return Observable.switchOnNext(
                rxServer.map(server -> {
                    /*
                    A note on thread safety. The initial status scan occurs on one thread, before
                    any concurrency contention. Subsequent modifications happen on the Rx io
                    scheduler, which by may be on different threads for each poll. Thus, not only
                    does the map need to be thread-safe, but also each mount record. The simplest
                    way to ensure this is just to lock map get/create and record access on a mutex,
                    which might as well be the map itself. This does perform some unnecessary
                    synchronization, but it all occurs on a worker thread on a low-fidelity loop, so
                    it's not worth a more sophisticated lock.

                    A fully correct minimal lock would include a thread-safe map and a read/write
                    lock for each record.
                     */
                    final Map<MountStatusKey, MountRecord> mountRecords = new HashMap<>();
                    final MountStatus[] mounts = server.getStatus().getMounts();
                    final List<MountEvent> alreadyMounted = new ArrayList<>(mounts.length);
                    for (final MountStatus status : mounts) {
                        if (status.getName().equals(name)) {
                            mountRecords.put(MountStatusKey.fromMountStatus(status),
                                    MountRecord.fromStatus(status));
                            if (isAlreadyMounted(status)) {
                                alreadyMounted.add(MountEvent.forStatus(true, status.getName(),
                                        status.getServer(), status.getLastMount(),
                                        status.getLastMountError() /* null */));
                            }
                        }
                    }
                    if (alreadyMounted.isEmpty()) {
                        try {
                            server.addName(name);
                        } catch (final VException e) {
                            return Observable.just(MountEvent.forAddNameFailure(name, e));
                        }
                        final MountEvent initial = MountEvent.forAddNameSuccess(name);

                        return RxMountState.index(
                                RxMountState.poll(server).map(state -> state.filter(
                                        status -> status.getName().equals(name))))
                                .flatMapIterable(Map::entrySet)
                                .concatMap(e -> processStatus(mountRecords,
                                        e.getKey(), e.getValue()))
                                .startWith(initial);
                    } else {
                        return Observable.from(alreadyMounted);
                    }
                }))
                .takeWhile(MountEvent::isMount);
    }
}
