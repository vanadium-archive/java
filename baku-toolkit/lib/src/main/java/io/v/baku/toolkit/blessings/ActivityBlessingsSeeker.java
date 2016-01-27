// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.blessings;

import android.app.Activity;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import net.javacrumbs.futureconverter.guavarx.FutureConverter;

import io.v.android.v23.services.blessing.BlessingCreationException;
import io.v.baku.toolkit.ErrorReporter;
import io.v.baku.toolkit.R;
import io.v.v23.security.Blessings;
import io.v.v23.verror.VException;
import lombok.Getter;
import lombok.Synchronized;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;
import rx.subjects.PublishSubject;

@Accessors(prefix = "m")
@Slf4j
public abstract class ActivityBlessingsSeeker implements RefreshableBlessingsProvider {
    /**
     * An observable that, when subscribed to, refreshes the blessing. If the account manager needs
     * to be invoked for the refresh, the subscription will not produce results until the invocation
     * completes. Subsequently, it will receive all blessings refreshed via
     * {@link #refreshBlessings()} and other subscriptions to {@link #getRxBlessings()}.
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

    private final Activity mActivity;
    private final ErrorReporter mErrorReporter;
    private final PublishSubject<ListenableFuture<Blessings>> mPub;
    private Blessings mLastBlessings;
    private SettableFuture<Blessings> mCurrentSeek;
    private final Object mSeekLock = new Object();

    public ActivityBlessingsSeeker(final Activity activity, final ErrorReporter errorReporter,
                                   final boolean seekInProgress) {
        mActivity = activity;
        mErrorReporter = errorReporter;

        if (seekInProgress) {
            mCurrentSeek = SettableFuture.create();
        }

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

    @Override
    @Synchronized("mSeekLock")
    public ListenableFuture<Blessings> refreshBlessings() {
        if (isAwaitingBlessings()) {
            return mCurrentSeek;
        }

        Blessings mgrBlessings;
        try {
            mgrBlessings = BlessingsUtils.readSharedPrefs(mActivity.getApplicationContext());
        } catch (final VException e) {
            log.warn("Could not get blessings from shared preferences", e);
            mgrBlessings = null;
        }

        final ListenableFuture<Blessings> nextBlessings;

        if (mgrBlessings == null) {
            nextBlessings = mCurrentSeek = SettableFuture.create();
            seekBlessings();
        } else {
            nextBlessings = Futures.immediateFuture(mgrBlessings);
        }
        mPub.onNext(nextBlessings);

        return nextBlessings;
    }

    protected abstract void seekBlessings();

    /**
     * It is an error to call this method when this instance is not awaiting blessings.
     */
    public void handleBlessingsError(final Throwable t) {
        if (t instanceof BlessingCreationException) {
            /* This exception can occur if a user hits "Deny" in Blessings Manager, so don't treat
            it as an error if we have a fallback. */
            if (mLastBlessings == null) {
                mErrorReporter.onError(R.string.err_blessings_required, t);
            } else {
                log.warn("Could not create blessings", t);
            }
        } else if (t instanceof VException) {
            mErrorReporter.onError(R.string.err_blessings_decode, t);
        } else {
            mCurrentSeek.setException(t);
            synchronized (mSeekLock) {
                mCurrentSeek = null;
            }
            return;
        }

        if (mLastBlessings == null) {
            mActivity.finish();
            /* Block while the app exits, as opposed to returning an error that would be reported
            (redundantly) elsewhere. */
        } else {
            setBlessings(mLastBlessings);
        }
    }

    /**
     * It is an error to call this method when this instance is not awaiting blessings.
     */
    public void setBlessings(final Blessings b) {
        try {
            BlessingsUtils.writeSharedPrefs(mActivity.getApplicationContext(), b);
        } catch (final VException e) {
            mErrorReporter.onError(R.string.err_blessings_store, e);
        } finally {
            mCurrentSeek.set(b);
            synchronized (mSeekLock) {
                mCurrentSeek = null;
            }
        }
    }
}
