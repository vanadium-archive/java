// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import org.joda.time.Duration;

import java.util.concurrent.TimeUnit;

import io.v.rx.syncbase.WatchEvent;
import lombok.RequiredArgsConstructor;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.ReplaySubject;

/**
 * If input events arrive too quickly, the resulting watch updates can come back in a stutter, where
 * older watch events arrive after the input has already been subsequently updated, reverting the
 * view for a brief moment and messing with the cursor position.
 * <p>
 * A simple debounce on the uplink or downlink doesn't solve the problem because it effectively just
 * adds a delay to the boundary condition. To prevent this, any update from the model must be
 * throttled if there was a recent update from the view.
 * <p>
 * Unfortunately for rapid concurrent updates this can result in divergence which should be handled
 * via conflict resolution or CRDT.
 */
@RequiredArgsConstructor
public class DebouncingCoordinator<T> implements TwoWayBinding<T> {
    public static final Duration DEFAULT_IO_DEBOUNCE = Duration.millis(500);

    private final TwoWayBinding<T> mChild;
    private final Duration mIoDebounce;

    private final ReplaySubject<Observable<?>> mRxDebounce = ReplaySubject.createWithSize(1);
    {
        mRxDebounce.onNext(Observable.just(0)
                .observeOn(AndroidSchedulers.mainThread()));
        //We expect these timeouts to be on the main thread; see putDebounceWindow
    }

    public DebouncingCoordinator(final TwoWayBinding<T> child) {
        this(child, DEFAULT_IO_DEBOUNCE);
    }

    private Observable<?> getDebounceWindow() {
        return Observable.switchOnNext(mRxDebounce).first();
    }

    private void putDebounceWindow() {
        mRxDebounce.onNext(Observable.timer(mIoDebounce.getMillis(), TimeUnit.MILLISECONDS,
                AndroidSchedulers.mainThread()));
        //Do timeouts on the main thread to ensure that timeouts don't clear while an input update
        //is in progress.
    }

    @Override
    public Observable<WatchEvent<T>> downlink() {
        return mChild.downlink().debounce(s -> getDebounceWindow());
    }

    @Override
    public Subscription uplink(final Observable<T> rxData) {
        return mChild.uplink(rxData.doOnNext(d -> putDebounceWindow()));
    }
}
