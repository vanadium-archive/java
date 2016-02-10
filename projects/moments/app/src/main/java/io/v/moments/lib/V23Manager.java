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
 * Various static V23 utilities gathered into an instantiatable class.
 *
 * This allows v23 usage to be injected and mocked.  The class is a singleton to
 * avoid confusion about init, shutdown and the 'base context'.
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

    public void advertise(
            Service advertisement,
            Duration duration,
            FutureCallback<VContext> startupCallback,
            FutureCallback<Void> completionCallback) {
        if (mDiscovery == null) {
            startupCallback.onFailure(
                    new IllegalStateException("Discovery not ready."));
            return;
        }
        VContext context = contextWithTimeout(duration);
        ListenableFuture<ListenableFuture<Void>> hey =
                mDiscovery.advertise(context, advertisement, NO_PATTERNS);
        Futures.addCallback(hey, makeWrapped(context, startupCallback, completionCallback));
    }

    /**
     * This exists to allow the scan and advertise interfaces to behave the same
     * way to clients, and to deliver the context via the "success" callback
     * so there's no chance of a race condition between usage of that context
     * and code in the callback.
     */
    private FutureCallback<ListenableFuture<Void>> makeWrapped(
            final VContext context,
            final FutureCallback<VContext> startup,
            final FutureCallback<Void> completion) {
        return new FutureCallback<ListenableFuture<Void>>() {
            @Override
            public void onSuccess(ListenableFuture<Void> result) {
                startup.onSuccess(context);
                Futures.addCallback(result, completion);
            }

            @Override
            public void onFailure(final Throwable t) {
                startup.onFailure(t);
            }
        };
    }


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

    public void shutdown() {
        Log.d(TAG, "Shutdown");
        if (mAndroidCtx == null) {
            Log.d(TAG, "Was never initialized.");
            return;
        }
        mV23Ctx.cancel();
        mAndroidCtx = null;
    }

    public VContext makeServerContext(
            String mountName, Object server) throws VException {
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
