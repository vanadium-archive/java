// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.view.View;
import android.widget.TextView;

import org.joda.time.Duration;

import java.util.concurrent.TimeUnit;

import io.v.baku.toolkit.BakuActivityTrait;
import io.v.rx.syncbase.SingleWatchEvent;
import lombok.RequiredArgsConstructor;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.ReplaySubject;

/**
 * This coordinator defers the read binding until a specified delay after the latest write, then
 * taking the latest read. Write/watch latency can cause reflexive watch changes from Syncbase to
 * arrive after subsequent changes to the UI state have already been made, causing a stuttering
 * revert.
 *
 * The default delay is 500 ms.
 *
 * A simple debounce on the read link or write link doesn't solve the problem because it effectively
 * just adds a delay to the boundary condition. To prevent this, any update from the model must be
 * throttled if there was a recent update from the view.
 *
 * Unfortunately for rapid concurrent updates this can result in divergence which should be handled
 * via conflict resolution or CRDT.
 *
 * This coordinator is included in the default coordinator chain for
 * {@linkplain ScalarBindingBuilder#bindTo(TextView) two-way
 * <code>TextView</code> bindings}.
 *
 * ## Usage
 *The following example shows how you would use this coordinator explicitly while creating a custom
 * binding within a {@link io.v.baku.toolkit.BakuActivityTrait}:
 *
 * ```java
 * {@link BakuActivityTrait#dataBinder() dataBinder()}.{@link BindingBuilder#forKey(java.lang.String)
 *     forKey}("foo")
 *             .{@link ScalarBindingBuilder#coordinators(CoordinatorChain[])
 *             coordinators}({@link
 *             DeferReadOnWriteCoordinator#DeferReadOnWriteCoordinator(TwoWayBinding)
 *             DeferReadOnWriteCoordinator::new})
 *             .{@link ScalarBindingBuilder#bindTo(View) bindTo}(myView);
 * ```
 */
@RequiredArgsConstructor
public class DeferReadOnWriteCoordinator<T> implements TwoWayBinding<T> {
    public static final Duration DEFAULT_IO_DEBOUNCE = Duration.millis(500);

    private final TwoWayBinding<T> mChild;
    private final Duration mIoDebounce;

    private final ReplaySubject<Observable<?>> mRxDebounce = ReplaySubject.createWithSize(1);
    {
        mRxDebounce.onNext(Observable.just(0)
                .observeOn(AndroidSchedulers.mainThread()));
        //We expect these timeouts to be on the main thread; see putDebounceWindow
    }

    /**
     * A reference to this constructor can be used as a {@link CoordinatorChain}.
     */
    public DeferReadOnWriteCoordinator(final TwoWayBinding<T> child) {
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
    public Observable<SingleWatchEvent<T>> linkRead() {
        return mChild.linkRead().debounce(s -> getDebounceWindow());
    }

    @Override
    public Subscription linkWrite(final Observable<T> rxData) {
        return mChild.linkWrite(rxData.doOnNext(d -> putDebounceWindow()));
    }
}
