// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.blessings;

import com.google.common.util.concurrent.ListenableFuture;

import net.javacrumbs.futureconverter.guavarx.FutureConverter;

import io.v.v23.security.Blessings;
import lombok.Getter;
import lombok.Synchronized;
import lombok.experimental.Accessors;
import rx.Observable;
import rx.subjects.PublishSubject;

@Accessors(prefix = "m")
public abstract class AbstractRefreshableBlessingsProvider implements RefreshableBlessingsProvider {
    /**
     * An observable that, when subscribed to, refreshes the blessing. If the
     * {@link io.v.android.libs.security.BlessingsManager} needs to be invoked for the refresh, the
     * subscription will not produce results until the invocation completes. Subsequently, it will
     * receive all blessings refreshed via {@link #refreshBlessings()} and other subscriptions to
     * {@link #getRxBlessings()}.
     */
    @Getter
    private final Observable<Blessings> mRxBlessings;
    /**
     * An observable for the blessings that does not refresh when subscribed to. Upon subscription,
     * this will produce the last known blessing. It will subsequently receive all blessings
     * refreshed via {@link #refreshBlessings()} and subscriptions to {@link #getRxBlessings()}.
     */
    @Getter
    private final Observable<Blessings> mPassiveRxBlessings;

    private final PublishSubject<ListenableFuture<Blessings>> mPub;
    private Blessings mLastBlessings;
    private ListenableFuture<Blessings> mCurrentSeek;
    private final Object mSeekLock = new Object();

    public AbstractRefreshableBlessingsProvider() {
        this(null);
    }

    public AbstractRefreshableBlessingsProvider(final ListenableFuture<Blessings> seekInProgress) {
        mCurrentSeek = seekInProgress;

        mPub = PublishSubject.create();
        mPassiveRxBlessings = mPub
                .flatMap(FutureConverter::toObservable)
                .distinctUntilChanged()
                .replay(1).autoConnect();
        /* It might make more sense for b -> mLastBlessings = b to be an onNext before the above
        replay rather than a subscription (especially if we start getting
        OnErrorNotImplementedException or have to include a possibly redundant error reporter).
        However, replay, even with autoConnect(0), does not offer backpressure support unless it has
        subscribers. We can get around this by adding .onBackpressureBuffer(1), but if this turns
        out to be a better way of doing this, we should submit an issue requesting that
        OperatorReplay use its buffer size for backpressure. */
        mPassiveRxBlessings.subscribe(b -> mLastBlessings = b);
        mRxBlessings = Observable.defer(() -> FutureConverter.toObservable(refreshBlessings()))
                .ignoreElements()
                .concatWith(mPassiveRxBlessings);
    }

    @Synchronized("mSeekLock")
    public boolean isAwaitingBlessings() {
        return mCurrentSeek != null;
    }

    protected abstract ListenableFuture<Blessings> handleBlessingsRefresh();

    @Override
    @Synchronized("mSeekLock")
    public ListenableFuture<Blessings> refreshBlessings() {
        if (isAwaitingBlessings()) {
            return mCurrentSeek;
        }

        final ListenableFuture<Blessings> nextBlessings = handleBlessingsRefresh();
        mPub.onNext(nextBlessings);
        return nextBlessings;
    }
}
