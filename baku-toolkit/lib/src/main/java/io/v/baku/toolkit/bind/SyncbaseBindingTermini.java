// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import java.util.Objects;

import io.v.rx.VFn;
import io.v.rx.syncbase.RxTable;
import io.v.rx.syncbase.WatchEvent;
import io.v.v23.syncbase.nosql.Table;
import lombok.AllArgsConstructor;
import lombok.experimental.UtilityClass;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

@UtilityClass
public class SyncbaseBindingTermini {
    @AllArgsConstructor
    private static class WriteData<T> {
        public final Table t;
        public final T data;
    }

    public static <T> Observable<WatchEvent<T>> bindRead(
            final RxTable rxTable, final String key, final Class<T> type, final T defaultValue) {
        return rxTable.watch(key, type, defaultValue);
    }

    public static <T> Subscription bindWrite(
            final RxTable rxTable, final Observable<T> rxData, final String key,
            final Class<T> type, final T deleteValue, final Action1<Throwable> onError) {
        return rxData
                .onBackpressureLatest()
                .observeOn(Schedulers.io())
                .switchMap(data -> rxTable.once().map(t -> new WriteData<>(t, data)))
                .subscribe(VFn.unchecked(w -> {
                    if (Objects.equals(w.data, deleteValue)) {
                        w.t.delete(rxTable.getVContext(), key);
                    } else {
                        w.t.put(rxTable.getVContext(), key, w.data, type);
                    }
                }), onError);
    }

    public static <T> TwoWayBinding<T> bind(
            final RxTable rxTable, final String key, final Class<T> type, final T defaultValue,
            final T deleteValue, final Action1<Throwable> onError) {
        return new TwoWayBinding<T>() {
            @Override
            public Observable<WatchEvent<T>> downlink() {
                return bindRead(rxTable, key, type, defaultValue);
            }

            @Override
            public Subscription uplink(final Observable<T> rxData) {
                return bindWrite(rxTable, rxData, key, type, deleteValue, onError);
            }
        };
    }
}
