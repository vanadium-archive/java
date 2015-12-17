// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.lib;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import org.joda.time.Duration;

import java.util.List;

import io.v.android.libs.security.BlessingsManager;
import io.v.android.v23.V;
import io.v.android.v23.services.blessing.BlessingCreationException;
import io.v.android.v23.services.blessing.BlessingService;
import io.v.v23.context.CancelableVContext;
import io.v.v23.context.VContext;
import io.v.v23.discovery.Service;
import io.v.v23.discovery.VDiscovery;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.Server;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;
import io.v.v23.security.VPrincipal;
import io.v.v23.security.VSecurity;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * Various static V23 utilities gathered in an injectable class.
 */
public class V23Manager {
    public static final int BLESSING_REQUEST = 201;
    private static final String TAG = "V23Manager";
    private Context mAndroidCtx;
    private VContext mV23Ctx = null;
    private Blessings mBlessings = null;

    // Singleton.
    private V23Manager() {
    }

    private static Blessings loadBlessings(Context context) {
        Log.d(TAG, "loadBlessings from prefs");
        try {
            // See if there are blessings stored in shared preferences.
            return BlessingsManager.getBlessings(context);
        } catch (VException e) {
            Log.w(TAG, "Cannot get blessings from prefs: " + e.getMessage());
        }
        return null;
    }

    /**
     * To be called from an Activity's onActivityResult method, e.g. public void
     * onActivityResult( int requestCode, int resultCode, Intent data) { if
     * (V23Manager.onActivityResult( getApplicationContext(), requestCode,
     * resultCode, data)) { return; } }
     */
    public static boolean onActivityResult(
            Context androidCtx, int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult; request = " + requestCode + ", result=" + resultCode);
        if (requestCode != BLESSING_REQUEST) {
            return false;
        }
        try {
            Log.d(TAG, "unpacking blessing");
            Blessings blessings = unpackBlessings(androidCtx, resultCode, data);
            Singleton.get().configurePrincipal(blessings);
        } catch (BlessingCreationException e) {
            throw new IllegalStateException(e);
        } catch (VException e) {
            throw new IllegalStateException(e);
        }
        return true;
    }

    private static Blessings unpackBlessings(
            Context androidCtx, int resultCode, Intent data)
            throws BlessingCreationException, VException {
        byte[] blessingsVom = BlessingService.extractBlessingReply(
                resultCode, data);
        Blessings blessings = (Blessings) VomUtil.decode(
                blessingsVom, Blessings.class);
        BlessingsManager.addBlessings(androidCtx, blessings);
        Toast.makeText(androidCtx, "Got blessings", Toast.LENGTH_SHORT).show();
        return blessings;
    }

    public CancelableVContext getCancellableContext(Duration d) {
        return mV23Ctx.withTimeout(d);
    }

    public VDiscovery getDiscovery() {
        return V.getDiscovery(mV23Ctx);
    }

    public CancelableVContext advertise(
            Service service, List<BlessingPattern> patterns, VDiscovery.AdvertiseDoneCallback cb) {
        CancelableVContext context = mV23Ctx.withCancel();
        Log.d(TAG, "Calling V.getDiscovery.advertise");
        V.getDiscovery(mV23Ctx).advertise(context, service, patterns, cb);
        Log.d(TAG, "Back from V.getDiscovery.advertise");
        return context;
    }

    public CancelableVContext scan(String query, VDiscovery.ScanCallback cb) {
        CancelableVContext context = mV23Ctx.withCancel();
        V.getDiscovery(mV23Ctx).scan(context, query, cb);
        return context;
    }

    public synchronized void init(Context androidCtx, Activity activity) {
        Log.d(TAG, "init");
        if (mAndroidCtx != null) {
            if (mAndroidCtx == androidCtx) {
                Log.d(TAG, "Initialization already started.");
                return;
            } else {
                Log.d(TAG, "Initialization with new context.");
                shutdown();
            }
        }
        mAndroidCtx = androidCtx;
        // Must call V.init before attempting to load blessings, so that proper
        // code is loaded.
        mV23Ctx = V.init(mAndroidCtx);
        Blessings blessings = loadBlessings(androidCtx);
        if (blessings == null) {
            Intent intent = BlessingService.newBlessingIntent(androidCtx);
            Log.d(TAG, "No blessings - navigating to " + intent.getComponent().getClassName());
            // Bail out and go get them, and re-enter init with them.
            if (activity == null) {
                throw new IllegalArgumentException(
                        "Cannot get blessings without an activity to return to.");
            }
            activity.startActivityForResult(intent, BLESSING_REQUEST);
            return;
        }
        configurePrincipal(blessings);
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

    private void configurePrincipal(final Blessings blessings) {
        Log.d(TAG, "configurePrincipal: blessings=" +
                (blessings == null ? "null" : blessings.toString()));
        try {
            VPrincipal p = V.getPrincipal(mV23Ctx);
            p.blessingStore().setDefaultBlessings(blessings);
            p.blessingStore().set(blessings, new BlessingPattern("..."));
            VSecurity.addToRoots(p, blessings);
            mBlessings = blessings;
        } catch (VException e) {
            Log.e(TAG, String.format(
                    "Couldn't set local blessing %s: %s",
                    blessings, e.getMessage()));
        }
        Log.d(TAG, "blessings stored: " +
                (mBlessings == null ? "NONE!" : mBlessings.toString()));
    }

    public void shutdown() {
        Log.d(TAG, "Shutdown");
        if (mAndroidCtx == null) {
            Log.d(TAG, "Was never initialized.");
            return;
        }
        V.shutdown();
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
            Log.d(TAG, "spec proxy: " + spec.getProxy().toString());
            return V.withListenSpec(mV23Ctx, spec);
        }
        return mV23Ctx;
    }

    public Server makeServer(String mountName, Object server) throws VException {
        return V.getServer(
                V.withNewServer(
                        getListenContext(),
                        mountName,
                        server,
                        VSecurity.newAllowEveryoneAuthorizer()));
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
