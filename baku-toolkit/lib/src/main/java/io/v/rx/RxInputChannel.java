// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import io.v.v23.InputChannel;
import io.v.v23.verror.EndOfFileException;
import lombok.experimental.UtilityClass;
import rx.Observable;
import rx.Subscriber;
import rx.observables.ConnectableObservable;

@UtilityClass
public class RxInputChannel {
    /**
     * Wraps an {@link io.v.v23.InputChannel} in a connectable observable that produces the same
     * elements.
     */
    public static <T> ConnectableObservable<T> wrap(final InputChannel<T> i) {
        return Observable.<T>create(s -> connect(i, s)).publish();
    }

    private static <T> void connect(final InputChannel<T> i, final Subscriber<? super T> s) {
        Futures.addCallback(i.recv(), new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                s.onNext(result);
                connect(i, s);
            }

            @Override
            public void onFailure(Throwable t) {
                if (t instanceof EndOfFileException) {
                    s.onCompleted();
                } else {
                    s.onError(t);
                }
            }
        });
    }
}
