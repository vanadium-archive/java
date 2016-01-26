// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.lib;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.joda.time.Duration;

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
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.Server;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;
import io.v.v23.security.VSecurity;
import io.v.v23.verror.VException;

/**
 * Various static V23 utilities gathered in an injectable class.
 */
public class V23Manager {
    private static final String BLESSINGS_KEY = "BlessingsKey";

    public static final int BLESSING_REQUEST = 201;
    private static final String TAG = "V23Manager";
    private Context mAndroidCtx;
    private VContext mV23Ctx = null;
    private Blessings mBlessings = null;

    // Singleton.
    private V23Manager() {
    }

    public VDiscovery getDiscovery() {
        return V.getDiscovery(mV23Ctx);
    }

    public VContext advertise(Service service, List<BlessingPattern> patterns) {
        VContext context = mV23Ctx.withCancel();
        Log.d(TAG, "Calling V.getDiscovery.advertise");
        V.getDiscovery(mV23Ctx).advertise(context, service, patterns);
        Log.d(TAG, "Back from V.getDiscovery.advertise");
        return context;
    }

    public VContext scan(String query, final ScanListener listener) {
        VContext context = mV23Ctx.withCancel();
        InputChannels.withCallback(V.getDiscovery(mV23Ctx).scan(context, query),
                new InputChannelCallback<Update>() {
                    @Override
                    public ListenableFuture<Void> onNext(Update result) {
                        listener.scanUpdateReceived(result);
                        return Futures.immediateFuture(null);
                    }
                });
        return context;
    }

    public synchronized ListenableFuture<Blessings> init(Context androidCtx, Activity activity) {
        Log.d(TAG, "init");
        if (mAndroidCtx != null) {
            if (mAndroidCtx == androidCtx) {
                Log.d(TAG, "Initialization already started.");
                return null;
            } else {
                Log.d(TAG, "Initialization with new context.");
                shutdown();
            }
        }
        mAndroidCtx = androidCtx;
        // Must call V.init before attempting to load blessings, so that proper
        // code is loaded.
        mV23Ctx = V.init(mAndroidCtx);
        return BlessingsManager.getBlessings(mV23Ctx, activity, "", true);
    }

    /**
     * v23 operations that require a blessing (almost everything) will fail if
     * attempted before this is true.
     *
     * The simplest usage is 1) There are no blessings. 2) An activity starts
     * and calls V23Manager.init. 2) init notices there are no blessings and
     * calls startActivityForResult 3) meanwhile, the activity and/or its
     * components still run, but can test isBlessed before attempting anything
     * requiring blessings. The activity will soon be re-initialized anyway. 4)
     * user kicked over into 'account manager', gets a blessing, and the
     * activity is restarted, this time with isBlessed == true.
     */
    public boolean isBlessed() {
        return mBlessings != null;
    }

    /**
     * Returns the blessings for this process.
     */
    public Blessings getBlessings() {
        return mBlessings;
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

    private VContext getListenContext() throws VException {
        final boolean useProxy = false;
        // Disabled while debugging network performance / visibility issues.
        if (useProxy) {
            ListenSpec spec = V.getListenSpec(mV23Ctx).withProxy("proxy");
            //ListenSpec spec = V.getListenSpec(mV23Ctx).withAddress(
            //        new ListenSpec.Address("tcp", "0.0.0.0:0"));
            Log.d(TAG, "spec : " + spec.toString());
            Log.d(TAG, "spec proxy: " + spec.getProxy());
            return V.withListenSpec(mV23Ctx, spec);
        }
        return mV23Ctx;
    }

    public VContext makeServer(String mountName, Object server) throws VException {
        return V.withNewServer(
                        getListenContext(),
                        mountName,
                        server,
                        VSecurity.newAllowEveryoneAuthorizer());
    }

    public Server getServer(VContext mServerCtx) {
        return V.getServer(mServerCtx);
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

    public VContext contextWithTimeout(Duration timeout) {
        return mV23Ctx.withTimeout(timeout);
    }
}
