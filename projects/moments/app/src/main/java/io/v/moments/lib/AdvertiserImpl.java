// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.lib;

import android.util.Log;

import com.google.common.util.concurrent.FutureCallback;

import org.joda.time.Duration;

import io.v.moments.ifc.AdSupporter;
import io.v.moments.ifc.Advertiser;
import io.v.v23.context.VContext;
import io.v.v23.verror.VException;

/**
 * Manages one instance of advertising - one ad, one backing service.
 *
 * This class hides/manages the two v23 contexts necessary to run a service and
 * a related advertisement.
 *
 * This class should have no knowledge of UX, and UX should have no access to
 * VContext, because said context is under active development, and it's too
 * complex to cover possible interactions with tests.
 */
public class AdvertiserImpl implements Advertiser {
    private static final String TAG = "AdvertiserImpl";

    private final V23Manager mV23Manager;
    private final AdSupporter mAdSupporter;
    private final Duration mDuration;

    private VContext mAdvCtx;
    private VContext mServerCtx;

    public AdvertiserImpl(
            V23Manager v23Manager, AdSupporter adSupporter, Duration duration) {
        if (v23Manager == null) {
            throw new IllegalArgumentException("Null v23Manager");
        }
        if (adSupporter == null) {
            throw new IllegalArgumentException("Null adSupporter");
        }
        if (duration == null) {
            duration = Duration.standardMinutes(5);
        }
        mV23Manager = v23Manager;
        mAdSupporter = adSupporter;
        mDuration = duration;
    }

    @Override
    public void start(
            FutureCallback<Void> startupCallback,
            FutureCallback<Void> completionCallback) {
        Log.d(TAG, "Entering start.");
        if (isAdvertising()) {
            startupCallback.onFailure(
                    new IllegalStateException("Already advertising."));
            return;
        }
        try {
            mServerCtx = mV23Manager.makeServerContext(
                    mAdSupporter.getMountName(),
                    mAdSupporter.makeServer());
        } catch (VException e) {
            startupCallback.onFailure(
                    new IllegalStateException("Unable to start service.", e));
            return;
        }
        mV23Manager.advertise(
                mAdSupporter.makeAdvertisement(
                        mV23Manager.makeServerAddressList(mServerCtx)),
                mDuration,
                makeWrapped(startupCallback),
                completionCallback);
        Log.d(TAG, "Exiting start.");
    }

    @Override
    public boolean isAdvertising() {
        return mAdvCtx != null && !mAdvCtx.isCanceled();
    }

    /**
     * A service must be started before advertising begins, which creates a
     * cleanup problem should advertising fail to start. This callback accepts
     * the advertising context on startup success, and cancels the already
     * started service on failure. This wrapper necessary to cleanly kill the
     * service should advertising fail to start.  This callback feeds info to
     * the startup callback, which presumably has some effect on UX.
     */
    private FutureCallback<VContext> makeWrapped(
            final FutureCallback<Void> startup) {
        return new FutureCallback<VContext>() {
            @Override
            public void onSuccess(VContext context) {
                mAdvCtx = context;
                startup.onSuccess(null);
            }

            @Override
            public void onFailure(final Throwable t) {
                mAdvCtx = null;
                cancelService();
                startup.onFailure(t);
            }
        };
    }

    @Override
    public void stop() {
        if (!isAdvertising()) {
            throw new IllegalStateException("Not advertising.");
        }
        Log.d(TAG, "Entering stop");
        if (mAdvCtx != null) {
            Log.d(TAG, "Cancelling advertising.");
            if (!mAdvCtx.isCanceled()) {
                mAdvCtx.cancel();
            }
            mAdvCtx = null;
        }
        cancelService();
        Log.d(TAG, "Exiting stop");
    }

    private void cancelService() {
        if (mServerCtx != null) {
            Log.d(TAG, "Cancelling service.");
            if (!mServerCtx.isCanceled()) {
                mServerCtx.cancel();
            }
            mServerCtx = null;
        }
    }

}
