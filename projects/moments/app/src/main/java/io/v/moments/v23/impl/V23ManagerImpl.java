// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.v23.impl;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.List;

import io.v.android.libs.security.BlessingsManager;
import io.v.android.v23.V;
import io.v.moments.v23.ifc.AdSupporter;
import io.v.moments.v23.ifc.Advertiser;
import io.v.moments.v23.ifc.V23Manager;
import io.v.v23.InputChannelCallback;
import io.v.v23.InputChannels;
import io.v.v23.context.VContext;
import io.v.v23.discovery.Update;
import io.v.v23.discovery.VDiscovery;
import io.v.v23.naming.Endpoint;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;
import io.v.v23.security.VSecurity;
import io.v.v23.verror.VException;

/**
 * Various static V23Manager utilities gathered into an instantiatable class.
 *
 * This allows v23 usage to be injected and mocked.  The class is a singleton to
 * avoid confusion about init, shutdown and the 'base context'.
 *
 * Nothing in here has anything in particular to do with Moments; its generic
 * code that hides an evolving, static API.
 */
public class V23ManagerImpl implements V23Manager {
    private static final String TAG = "V23ManagerImpl";
    private static final String BLESSINGS_KEY = "BlessingsKey";
    private Context mAndroidCtx;
    private VContext mV23Ctx = null;
    private VDiscovery mDiscovery = null;

    // Constructor should only be called from tests.
    /* package private */ V23ManagerImpl() {
    }

    @Override
    public void scan(
            String query,
            Duration duration,
            FutureCallback<VContext> startupCallback,
            InputChannelCallback<Update> updateCallback,
            FutureCallback<Void> completionCallback) {
        Log.d(TAG, "Starting scan with q=[" + query + "]");
        if (mDiscovery == null) {
            startupCallback.onFailure(
                    new IllegalStateException("Discovery not ready."));
            return;
        }
        VContext context = contextWithTimeout(duration);
        Futures.addCallback(
                InputChannels.withCallback(
                        mDiscovery.scan(context, query), updateCallback),
                completionCallback);
        startupCallback.onSuccess(context);
    }

    @Override
    public Advertiser makeAdvertiser(AdSupporter adSupporter,
                                     Duration duration,
                                     List<BlessingPattern> visibility) {
        return new AdvertiserImpl(adSupporter, duration, visibility);
    }

    @Override
    public synchronized void init(
            Activity activity, FutureCallback<Blessings> blessingCallback) {
        Log.d(TAG, "init");
        if (mAndroidCtx != null) {
            if (mAndroidCtx == activity.getApplicationContext()) {
                Log.d(TAG, "Initialization already started.");
                return;
            } else {
                Log.d(TAG, "Initialization with new context.");
                shutdown();
            }
        }
        mAndroidCtx = activity.getApplicationContext();
        // Must call V.init before attempting to load blessings, so that proper
        // code is loaded.
        mV23Ctx = V.init(mAndroidCtx);
        Log.d(TAG, "Attempting to get blessings.");
        ListenableFuture<Blessings> f = BlessingsManager.getBlessings(
                mV23Ctx, activity, BLESSINGS_KEY, true);
        Futures.addCallback(f, blessingCallback);
        try {
            mDiscovery = V.newDiscovery(mV23Ctx);
        } catch (VException e) {
            Log.d(TAG, "Unable to get discovery object.", e);
        }
    }

    @Override
    public void shutdown() {
        Log.d(TAG, "Shutdown");
        if (mAndroidCtx == null) {
            Log.d(TAG, "Was never initialized.");
            return;
        }
        mV23Ctx.cancel();
        mAndroidCtx = null;
    }

    @Override
    public VContext makeServerContext(
            String mountName, Object server) throws VException {
        return V.withNewServer(
                mV23Ctx.withCancel(),
                mountName,
                server,
                VSecurity.newAllowEveryoneAuthorizer());
    }

    @Override
    public List<String> makeServerAddressList(VContext serverCtx) {
        List<String> addresses = new ArrayList<>();
        Endpoint[] points = V.getServer(
                serverCtx).getStatus().getEndpoints();
        for (Endpoint point : points) {
            addresses.add(point.toString());
        }
        return addresses;
    }

    @Override
    public VContext contextWithTimeout(Duration timeout) {
        return mV23Ctx.withTimeout(timeout);
    }

    public static class Singleton {
        private static volatile V23ManagerImpl instance;

        public static V23ManagerImpl get() {
            V23ManagerImpl result = instance;
            if (instance == null) {
                synchronized (Singleton.class) {
                    result = instance;
                    if (result == null) {
                        instance = result = new V23ManagerImpl();
                    }
                }
            }
            return result;
        }
    }

    /**
     * Manages one instance of advertising - one ad, one backing service.
     *
     * This class hides/manages the two v23 contexts necessary to run a service
     * and a related advertisement.
     */
    class AdvertiserImpl implements Advertiser {
        private static final String TAG = "AdvertiserImpl";

        private final AdSupporter mAdSupporter;
        private final Duration mDuration;
        private final List<BlessingPattern> mVisibility;

        private VContext mAdvCtx;
        private VContext mServerCtx;

        public AdvertiserImpl(
                AdSupporter adSupporter,
                Duration duration,
                List<BlessingPattern> visibility) {
            if (adSupporter == null) {
                throw new IllegalArgumentException("Null adSupporter");
            }
            if (duration == null) {
                throw new IllegalArgumentException("Null duration");
            }
            if (visibility == null) {
                throw new IllegalArgumentException("Null visibility");
            }
            mAdSupporter = adSupporter;
            mDuration = duration;
            mVisibility = visibility;
        }

        @Override
        public void start(FutureCallback<Void> onStartCallback,
                          FutureCallback<Void> onStopCallback) {
            Log.d(TAG, "Entering start.");
            if (isAdvertising()) {
                onStartCallback.onFailure(
                        new IllegalStateException("Already advertising."));
                return;
            }

            if (mDiscovery == null) {
                onStartCallback.onFailure(
                        new IllegalStateException("Discovery not ready."));
                return;
            }

            try {
                mServerCtx = makeServerContext(
                        mAdSupporter.getMountName(),
                        mAdSupporter.makeServer());
            } catch (VException e) {
                onStartCallback.onFailure(
                        new IllegalStateException("Unable to start service.", e));
                return;
            }


            VContext context = contextWithTimeout(mDuration);

            ListenableFuture<ListenableFuture<Void>> nestedFuture =
                    mDiscovery.advertise(
                            context,
                            mAdSupporter.makeAdvertisement(
                                    makeServerAddressList(mServerCtx)),
                            mVisibility);

            Futures.addCallback(
                    nestedFuture,
                    deliverContextCallback(
                            context,
                            confirmCleanStartCallback(onStartCallback),
                            onStopCallback));

            Log.d(TAG, "Exiting start.");
        }

        /**
         * A service must be started before advertising begins, which creates a
         * cleanup problem should advertising fail to start. This callback
         * accepts the advertising context on startup success, and cancels the
         * already started service on failure.
         *
         * This wrapper necessary to cleanly kill the service should advertising
         * fail to start.  This callback feeds info to the startup callback,
         * which presumably has some effect on UX.
         */
        private FutureCallback<VContext> confirmCleanStartCallback(
                final FutureCallback<Void> onStart) {
            return new FutureCallback<VContext>() {
                @Override
                public void onSuccess(VContext context) {
                    mAdvCtx = context;
                    onStart.onSuccess(null);
                }

                @Override
                public void onFailure(final Throwable t) {
                    mAdvCtx = null;
                    cancelService();
                    onStart.onFailure(t);
                }
            };
        }

        /**
         * This exists to allow the scan and advertise interfaces to behave the
         * same way to clients (accepting simple Callback<Void> for both onStart
         * and onStop), and to deliver the context via the "success" callback so
         * there's no chance of a race condition between usage of that context
         * and code in the onStart callback.
         */
        private FutureCallback<ListenableFuture<Void>> deliverContextCallback(
                final VContext context,
                final FutureCallback<VContext> onStart,
                final FutureCallback<Void> onStop) {
            return new FutureCallback<ListenableFuture<Void>>() {
                @Override
                public void onSuccess(ListenableFuture<Void> result) {
                    onStart.onSuccess(context);
                    Futures.addCallback(result, onStop);
                }

                @Override
                public void onFailure(final Throwable t) {
                    onStart.onFailure(t);
                }
            };
        }

        @Override
        public boolean isAdvertising() {
            return mAdvCtx != null && !mAdvCtx.isCanceled();
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
}
