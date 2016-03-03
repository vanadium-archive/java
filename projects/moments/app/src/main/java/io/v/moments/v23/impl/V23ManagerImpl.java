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
import org.joda.time.ReadableDuration;

import java.util.ArrayList;
import java.util.List;

import io.v.android.libs.inspectors.RemoteInspectors;
import io.v.android.libs.security.BlessingsManager;
import io.v.android.v23.V;
import io.v.moments.v23.ifc.AdCampaign;
import io.v.moments.v23.ifc.AdvertisementFoundListener;
import io.v.moments.v23.ifc.AdvertisementLostListener;
import io.v.moments.v23.ifc.Advertiser;
import io.v.moments.v23.ifc.Scanner;
import io.v.moments.v23.ifc.V23Manager;
import io.v.v23.InputChannelCallback;
import io.v.v23.InputChannels;
import io.v.v23.context.VContext;
import io.v.v23.discovery.Advertisement;
import io.v.v23.discovery.Discovery;
import io.v.v23.discovery.Update;
import io.v.v23.naming.Endpoint;
import io.v.v23.security.Blessings;
import io.v.v23.security.VSecurity;
import io.v.v23.verror.VException;

/**
 * Various static V23 utilities gathered into an instantiatable class.
 *
 * This allows V23 usage to be injected and mocked.  The class is a singleton to
 * avoid confusion about init, shutdown and the 'base context'.
 *
 * Nothing in here has anything in particular to do with Moments; it's an object
 * hiding an evolving API based on static functions.
 */
public class V23ManagerImpl implements V23Manager {
    private static final String TAG = "V23ManagerImpl";
    private static final String BLESSINGS_KEY = "BlessingsKey";
    private Context mAndroidCtx;
    private VContext mV23Ctx = null;
    private Discovery mDiscovery = null;
    private RemoteInspectors mInspectors = null;

    // Constructor should only be called from tests.
    /* package private */ V23ManagerImpl() {}

    @Override
    public synchronized void init(Activity activity, FutureCallback<Blessings> blessingCallback) {
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
        ListenableFuture<Blessings> f =
                BlessingsManager.getBlessings(mV23Ctx, activity, BLESSINGS_KEY, true);
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
        mInspectors = null;
        mV23Ctx.cancel();
        mAndroidCtx = null;
    }

    private VContext makeServerContext(String mountName, Object server) throws VException {
        return V.withNewServer(
                mV23Ctx.withCancel(), mountName, server, VSecurity.newAllowEveryoneAuthorizer());
    }

    private List<String> makeServerAddressList(VContext serverCtx) {
        List<String> addresses = new ArrayList<>();
        Endpoint[] points = V.getServer(serverCtx).getStatus().getEndpoints();
        for (Endpoint point : points) {
            addresses.add(point.toString());
        }
        return addresses;
    }

    @Override
    public VContext contextWithTimeout(Duration timeout) {
        return mV23Ctx.withTimeout(timeout);
    }

    @Override
    public Advertiser makeAdvertiser(AdCampaign adCampaign) {
        return new AdvertiserImpl(adCampaign);
    }

    @Override
    public Scanner makeScanner(String query) {
        return new ScannerImpl(query);
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

    @Override
    public void enableRemoteInspection() throws IllegalStateException {
        if (mInspectors != null) {
            return;
        }
        try {
            mInspectors = new RemoteInspectors(mV23Ctx);
        } catch (VException e) {
            throw new IllegalStateException("Unable to setup remote V23 inspection:" + e);
        }
    }

    @Override
    public String inviteInspector(String invitee, ReadableDuration duration)
            throws IllegalStateException {
        enableRemoteInspection();
        try {
            return mInspectors.invite(invitee, duration);
        } catch (VException e) {
            throw new IllegalStateException("Unable to invite V23 inspector(" + invitee + "):" + e);
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

        private final AdCampaign mAdCampaign;

        private VContext mAdvCtx;
        private VContext mServerCtx;
        private Duration mDuration;

        public AdvertiserImpl(AdCampaign adCampaign) {
            if (adCampaign == null) {
                throw new IllegalArgumentException("Null adCampaign");
            }
            mAdCampaign = adCampaign;
        }

        @Override
        public void start(
                FutureCallback<Void> onStartCallback,
                FutureCallback<Void> onStopCallback,
                Duration timeout) {
            Log.d(TAG, "Entering start.");
            if (isAdvertising()) {
                onStartCallback.onFailure(new IllegalStateException("Already advertising."));
                return;
            }

            if (timeout == null) {
                throw new IllegalArgumentException("Null timeout");
            }
            mDuration = timeout;

            if (mDiscovery == null) {
                onStartCallback.onFailure(new IllegalStateException("Discovery not ready."));
                return;
            }

            try {
                mServerCtx =
                        makeServerContext(mAdCampaign.getMountName(), mAdCampaign.makeService());
            } catch (VException e) {
                onStartCallback.onFailure(new IllegalStateException("Unable to start service.", e));
                return;
            }

            VContext context = contextWithTimeout(mDuration);
            Advertisement advertisement =
                    new Advertisement(
                            mAdCampaign.getId(),
                            mAdCampaign.getInterfaceName(),
                            makeServerAddressList(mServerCtx),
                            mAdCampaign.getAttributes(),
                            mAdCampaign.getAttachments());

            ListenableFuture<Void> future;
            try {
                future = mDiscovery.advertise(context, advertisement, mAdCampaign.getVisibility());
            } catch (VException e) {
                mAdvCtx = null;
                cancelService();
                onStartCallback.onFailure(
                        new IllegalStateException("Unable to advertise service.", e));
                return;
            }
            mAdvCtx = context;
            onStartCallback.onSuccess(null);
            Futures.addCallback(future, onStopCallback);
            Log.d(TAG, "Exiting start.");
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

    /**
     * Handles scanning for moments.
     *
     * To make this class more useful in decoupling moments specific code from
     * v23 specifics, this class could tease apart a scan Update, and feed
     * advertisement data from the Update.Found and Update.Lost object directly
     * into, say, the appropriate instances of java.util.function.Consumer<T>
     * that were passed to #start en lieu of the InputChannelCallback<Update>
     * updateCallback.  That way the caller would have no exposure to v23
     * classes (Update and Lost).
     */
    class ScannerImpl implements Scanner {
        private static final String TAG = "ScannerImpl";
        private final String mQuery;
        private Duration mDuration;
        private VContext mScanCtx;

        public ScannerImpl(String query) {
            if (query == null || query.isEmpty()) {
                throw new IllegalArgumentException("Empty query.");
            }
            mQuery = query;
        }

        @Override
        public String toString() {
            return "scan(" + mQuery + "," + isScanning() + ")";
        }

        @Override
        public void start(
                FutureCallback<Void> onStart,
                AdvertisementFoundListener foundListener,
                AdvertisementLostListener lostListener,
                FutureCallback<Void> onStop,
                Duration timeout) {
            Log.d(TAG, "Entering start.");
            if (isScanning()) {
                onStart.onFailure(new IllegalStateException("Already scanning."));
                return;
            }
            if (timeout == null) {
                throw new IllegalArgumentException("Null timeout.");
            }
            mDuration = timeout;
            Log.d(TAG, "Starting scan with q=[" + mQuery + "]");
            if (mDiscovery == null) {
                onStart.onFailure(new IllegalStateException("Discovery not ready."));
                return;
            }
            mScanCtx = contextWithTimeout(mDuration);

            try {
                Futures.addCallback(
                        InputChannels.withCallback(
                                mDiscovery.scan(mScanCtx, mQuery),
                                makeUpdateCallback(foundListener, lostListener)),
                        onStop);
            } catch (VException e) {
                onStart.onFailure(new IllegalStateException("Unable to scan.", e));
            }

            onStart.onSuccess(null);

            Log.d(TAG, "Exiting start.");
        }

        private InputChannelCallback<Update> makeUpdateCallback(
                final AdvertisementFoundListener foundListener,
                final AdvertisementLostListener lostListener) {
            return new InputChannelCallback<Update>() {
                @Override
                public ListenableFuture<Void> onNext(Update result) {
                    if (result.isLost()) {
                        lostListener.handleLostAdvertisement(result.getAdvertisement());
                    } else {
                        foundListener.handleFoundAdvertisement(result.getAdvertisement());
                    }
                    return Futures.immediateFuture(null);
                }
            };
        }

        @Override
        public boolean isScanning() {
            return mScanCtx != null && !mScanCtx.isCanceled();
        }

        @Override
        public void stop() {
            Log.d(TAG, "Entering stop");
            if (mScanCtx != null) {
                Log.d(TAG, "Cancelling scan.");
                if (!mScanCtx.isCanceled()) {
                    mScanCtx.cancel();
                }
                mScanCtx = null;
            }
            Log.d(TAG, "Exiting stop");
        }
    }
}
