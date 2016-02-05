// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.lib;

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
import io.v.moments.ifc.ScanListener;
import io.v.v23.InputChannelCallback;
import io.v.v23.InputChannels;
import io.v.v23.context.VContext;
import io.v.v23.discovery.Service;
import io.v.v23.discovery.Update;
import io.v.v23.discovery.VDiscovery;
import io.v.v23.rpc.Server;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;
import io.v.v23.security.VSecurity;
import io.v.v23.verror.VException;

/**
 * Various static V23 utilities gathered in an injectable class.
 */
public class V23Manager {
    private static final String TAG = "V23Manager";
    private static final String BLESSINGS_KEY = "BlessingsKey";
    private static final List<BlessingPattern> NO_PATTERNS = new ArrayList<>();
    private Context mAndroidCtx;
    private VContext mV23Ctx = null;
    private VDiscovery mDiscovery = null;

    // Singleton.
    private V23Manager() {
    }

    public VContext advertise(final Service service, FutureCallback<ListenableFuture<Void>> callback) {
        if (mDiscovery == null) {
            callback.onFailure(new IllegalStateException("Discovery not ready."));
            return null;
        }
        VContext context = mV23Ctx.withTimeout(Duration.standardMinutes(5));
        final ListenableFuture<ListenableFuture<Void>> fStart =
                mDiscovery.advertise(context, service, NO_PATTERNS);
        Futures.addCallback(fStart, callback);
        Log.d(TAG, "Back from V.getDiscovery.advertise");
        return context;
    }

    public VContext scan(String query, final ScanListener listener) {
        if (mDiscovery == null) {
            Log.d(TAG, "Discovery not ready.");
            return null;
        }
        VContext context = mV23Ctx.withCancel();
        Log.d(TAG, "Calling V.getDiscovery.scan with q=" + query);
        final ListenableFuture<Void> fStart =
                InputChannels.withCallback(mDiscovery.scan(context, query),
                        new InputChannelCallback<Update>() {
                            @Override
                            public ListenableFuture<Void> onNext(Update result) {
                                listener.scanUpdateReceived(result);
                                return Futures.immediateFuture(null);
                            }
                        });
        Futures.addCallback(fStart, new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Log.d(TAG, "Scan started.");
            }

            @Override
            public void onFailure(Throwable t) {
                Log.d(TAG, "Failure to start scan.", t);
            }
        });
        return context;
    }

    public synchronized void init(Activity activity, FutureCallback<Blessings> future) {
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
        Futures.addCallback(f, future);
        try {
            mDiscovery = V.newDiscovery(mV23Ctx);
        } catch (VException e) {
            Log.d(TAG, "Unable to get discovery object.", e);
        }
    }

    public void shutdown() {
        Log.d(TAG, "Shutdown");
        if (mAndroidCtx == null) {
            Log.d(TAG, "Was never initialized.");
            return;
        }
        mV23Ctx.cancel();
        mAndroidCtx = null;
    }

    public VContext makeServerContext(String mountName, Object server) throws VException {
        return V.withNewServer(
                mV23Ctx.withCancel(),
                mountName,
                server,
                VSecurity.newAllowEveryoneAuthorizer());
    }

    public Server getServer(VContext mServerCtx) {
        return V.getServer(mServerCtx);
    }

    public VContext contextWithTimeout(Duration timeout) {
        return mV23Ctx.withTimeout(timeout);
    }

    public static class Singleton {
        private static volatile V23Manager instance;

        public static V23Manager get() {
            V23Manager result = instance;
            if (instance == null) {
                synchronized (Singleton.class) {
                    result = instance;
                    if (result == null) {
                        instance = result = new V23Manager();
                    }
                }
            }
            return result;
        }
    }
}
