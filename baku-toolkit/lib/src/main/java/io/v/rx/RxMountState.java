// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx;

import org.joda.time.Duration;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.v.v23.rpc.MountStatus;
import io.v.v23.rpc.MountStatusKey;
import io.v.v23.rpc.MountStatusValue;
import io.v.v23.rpc.Server;
import java8.util.J8Arrays;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import lombok.experimental.UtilityClass;
import rx.Observable;

@UtilityClass
public class RxMountState {
    /**
     * Time interval for mount status polling.
     */
    public static final Duration DEFAULT_POLLING_INTERVAL = Duration.standardSeconds(1);

    public static Observable<Stream<MountStatus>> poll(final Server s, final Duration interval) {
        return Observable.interval(0, interval.getMillis(), TimeUnit.MILLISECONDS)
                .map(i -> J8Arrays.stream(s.getStatus().getMounts()));
    }

    public static Observable<Stream<MountStatus>> poll(final Server s) {
        return poll(s, DEFAULT_POLLING_INTERVAL);
    }

    public static Map<MountStatusKey, MountStatusValue> index(
            final Stream<MountStatus> state) {
        return state.collect(Collectors.toMap(
                MountStatusKey::fromMountStatus,
                MountStatusValue::fromMountStatus));
    }

    public static Map<MountStatusKey, MountStatusValue> index(
            final MountStatus[] state) {
        return index(J8Arrays.stream(state));
    }

    public static Observable<Map<MountStatusKey, MountStatusValue>> index(
            final Observable<Stream<MountStatus>> rx) {
        return rx.map(RxMountState::index)
                .distinctUntilChanged();
    }
}
