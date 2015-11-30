// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import io.v.rx.syncbase.WatchEvent;
import lombok.RequiredArgsConstructor;
import rx.Observable;
import rx.Subscription;

/**
 * If we don't suppress writes while responding to SB changes with Android widgets, we easily end
 * up in update loops. To operate correctly, this coordinator must occur single-threaded with the
 * widget binding layer.
 */
@RequiredArgsConstructor
public class SuppressWriteOnReadCoordinator<T> implements TwoWayBinding<T> {
    private final TwoWayBinding<T> mChild;

    private boolean mSuppressWrites;

    @Override
    public Observable<WatchEvent<T>> downlink() {
        final Observable<WatchEvent<T>> childDownlink = mChild.downlink();
        return Observable.create(s -> s.add(childDownlink.subscribe(x -> {
            mSuppressWrites = true;
            s.onNext(x);
            mSuppressWrites = false;
        }, s::onError, s::onCompleted)));
    }

    @Override
    public Subscription uplink(Observable<T> rxData) {
        return mChild.uplink(rxData.filter(x -> !mSuppressWrites));
    }
}
