// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Objects;

import io.v.rx.syncbase.RxTable;
import io.v.rx.syncbase.SingleWatchEvent;
import io.v.v23.syncbase.nosql.Table;
import lombok.AllArgsConstructor;
import lombok.experimental.UtilityClass;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;

@UtilityClass
public class SyncbaseBindingTermini {
    @AllArgsConstructor
    private static class WriteData<T> {
        public final Table t;
        public final T data;
    }

    public static <T> Observable<SingleWatchEvent<T>> bindRead(
            final RxTable rxTable, final String key, final Class<T> type, final T defaultValue) {
        return rxTable.watch(key, type, defaultValue);
    }

    public static <T> Subscription bindWrite(
            final RxTable rxTable, final Observable<T> rxData, final String key,
            final Class<T> type, final T deleteValue, final Action1<Throwable> onError) {
        return rxData
                .switchMap(data -> rxTable.once().map(t -> new WriteData<>(t, data)))
                .onBackpressureLatest()
                .subscribe(new Subscriber<WriteData<T>>() {
                    @Override
                    public void onStart() {
                        request(1);
                    }

                    @Override
                    public void onNext(WriteData<T> w) {
                        final ListenableFuture<Void> op = Objects.equals(w.data, deleteValue) ?
                                w.t.delete(rxTable.getVContext(), key) :
                                w.t.put(rxTable.getVContext(), key, w.data, type);
                        Futures.addCallback(op, new FutureCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                request(1);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                onError(t);
                            }
                        });
                    }

                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(final Throwable t) {
                        onError.call(t);
                    }
                });
    }

    public static <T> TwoWayBinding<T> bind(
            final RxTable rxTable, final String key, final Class<T> type, final T defaultValue,
            final T deleteValue, final Action1<Throwable> onError) {
        return new TwoWayBinding<T>() {
            @Override
            public Observable<SingleWatchEvent<T>> downlink() {
                return bindRead(rxTable, key, type, defaultValue);
            }

            @Override
            public Subscription uplink(final Observable<T> rxData) {
                return bindWrite(rxTable, rxData, key, type, deleteValue, onError);
            }
        };
    }
}
