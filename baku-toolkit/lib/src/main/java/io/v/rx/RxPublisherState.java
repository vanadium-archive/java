// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx;

import org.joda.time.Duration;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.v.v23.rpc.PublisherEntry;
import io.v.v23.rpc.PublisherEntryKey;
import io.v.v23.rpc.PublisherEntryValue;
import io.v.v23.rpc.Server;
import java8.util.J8Arrays;
import java8.util.stream.Collectors;
import java8.util.stream.Stream;
import lombok.experimental.UtilityClass;
import rx.Observable;

@UtilityClass
public class RxPublisherState {
    /**
     * Time interval for mount status polling.
     */
    public static final Duration DEFAULT_POLLING_INTERVAL = Duration.standardSeconds(1);

    public static Observable<Stream<PublisherEntry>> poll(final Server s, final Duration interval) {
        return Observable.interval(0, interval.getMillis(), TimeUnit.MILLISECONDS)
                .map(i -> J8Arrays.stream(s.getStatus().getPublisherStatus()));
    }

    public static Observable<Stream<PublisherEntry>> poll(final Server s) {
        return poll(s, DEFAULT_POLLING_INTERVAL);
    }

    public static Map<PublisherEntryKey, PublisherEntryValue> index(
            final Stream<PublisherEntry> state) {
        return state.collect(Collectors.toMap(
                PublisherEntryKey::fromPublisherEntry,
                PublisherEntryValue::fromPublisherEntry));
    }

    public static Map<PublisherEntryKey, PublisherEntryValue> index(
            final PublisherEntry[] state) {
        return index(J8Arrays.stream(state));
    }

    public static Observable<Map<PublisherEntryKey, PublisherEntryValue>> index(
            final Observable<Stream<PublisherEntry>> rx) {
        return rx.map(RxPublisherState::index)
                .distinctUntilChanged();
    }
}
