// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.app.Activity;

import io.v.android.libs.security.BlessingsManager;
import io.v.android.v23.services.blessing.BlessingCreationException;
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
public class BlessedActivityTrait implements BlessingsProvider {
    @Getter
    private final Activity mActivity;
    @Getter
    private final ErrorReporter mErrorReporter;

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

    private final PublishSubject<Observable<Blessings>> mPub;
    private Blessings mLastBlessings;
    private Observable<Blessings> mCurrentSeek;
    private final Object mSeekLock = new Object();

    public BlessedActivityTrait(final Activity activity, final ErrorReporter errorReporter) {
        mActivity = activity;
        mErrorReporter = errorReporter;

        mPub = PublishSubject.create();
        mPassiveRxBlessings = Observable.switchOnNext(mPub)
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
        mRxBlessings = Observable.defer(this::refreshBlessings)
                .ignoreElements()
                .concatWith(mPassiveRxBlessings);
    }

    @Synchronized("mSeekLock")
    public Observable<Blessings> refreshBlessings() {
        if (mCurrentSeek != null) {
            return mCurrentSeek;
        }

        Blessings mgrBlessings;
        try {
            mgrBlessings = BlessingsManager.getBlessings(mActivity.getApplicationContext());
        } catch (final VException e) {
            log.warn("Could not get blessings from shared preferences", e);
            mgrBlessings = null;
        }

        final Observable<Blessings> nextBlessings;

        if (mgrBlessings == null) {
            mCurrentSeek = nextBlessings = seekBlessings()
                    .first() // Longer-lived providers should consider a more direct implementation
                            // of BlessingsProvider.
                    .onErrorResumeNext(this::handleBlessingsError)
                    .doOnNext(this::afterSeekBlessings)
                    .replay(1).autoConnect();
        } else {
            nextBlessings = Observable.just(mgrBlessings);
        }
        mPub.onNext(nextBlessings);

        return nextBlessings;
    }

    protected Observable<Blessings> seekBlessings() {
        final BlessingRequestFragment brf = new BlessingRequestFragment();
        mActivity.getFragmentManager().beginTransaction()
                .add(brf, null)
                .commit();
        return brf.getObservable();
    }

    protected Observable<Blessings> handleBlessingsError(final Throwable t) {
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
            return Observable.error(t);
        }

        if (mLastBlessings == null) {
            mActivity.finish();
            /* Block while the app exits, as opposed to returning an error that would be reported
            (redundantly) elsewhere. */
            return Observable.never();
        } else {
            return Observable.just(mLastBlessings);
        }
    }

    private void afterSeekBlessings(final Blessings b) {
        try {
            BlessingsManager.addBlessings(mActivity.getApplicationContext(), b);
        } catch (final VException e) {
            mErrorReporter.onError(R.string.err_blessings_store, e);
        } finally {
            synchronized (mSeekLock) {
                mCurrentSeek = null;
            }
        }
    }
}
