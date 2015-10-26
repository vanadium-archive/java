// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.misc;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.v.android.apps.syncslides.SignInActivity;
import io.v.android.apps.syncslides.db.DB;
import io.v.android.libs.security.BlessingsManager;
import io.v.android.v23.V;
import io.v.android.v23.services.blessing.BlessingCreationException;
import io.v.android.v23.services.blessing.BlessingService;
import io.v.impl.google.naming.NamingUtil;
import io.v.v23.context.VContext;
import io.v.v23.namespace.Namespace;
import io.v.v23.naming.Endpoint;
import io.v.v23.naming.GlobReply;
import io.v.v23.naming.MountEntry;
import io.v.v23.naming.MountedServer;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerState;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;
import io.v.v23.security.VPrincipal;
import io.v.v23.security.VSecurity;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * Does vanadium stuff - MT scanning, service creation, unmounting, etc.
 *
 * This class is a singleton, since all vanadium activity must involve a
 * Vanadium context recovered from a static call to V.init, ultimately (ideally)
 * bookended by a static call to V.shutdown.   In an app.Service, one could call
 * these in onCreate and onDestroy respectively.
 */
public class V23Manager {
    public static final Duration MT_TIMEOUT =
            Duration.standardSeconds(10);
    public static final int BLESSING_REQUEST = 201;
    private static final String TAG = "V23Manager";
    private static final ExecutorService mExecutor =
            Executors.newSingleThreadExecutor();
    private Context mAndroidCtx;
    private VContext mBaseContext = null;
    private Blessings mBlessings = null;
    // Can only have one of these at the moment.  Could add more...
    private Server mLiveServer = null;

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
     * To be called from an Activity's onActivityResult method, e.g.
     *     public void onActivityResult(
     *         int requestCode, int resultCode, Intent data) {
     *         if (V23Manager.onActivityResult(
     *             getApplicationContext(), requestCode, resultCode, data)) {
     *           return;
     *         }
     *     }
     */
    public static boolean onActivityResult(
            Context androidCtx, int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult");
        if (requestCode != BLESSING_REQUEST) {
            return false;
        }
        try {
            Log.d(TAG, "unpacking blessing");
            Blessings blessings = unpackBlessings(androidCtx, resultCode, data);
            Singleton.get().configurePrincipal(blessings);
            DB.Singleton.get(androidCtx).init();
        } catch (BlessingCreationException e) {
            throw new IllegalStateException(e);
        } catch (VException e) {
            throw new IllegalStateException(e);
        }
        return true;
    }

    public static Blessings unpackBlessings(
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

    /**
     * @return IP address of the mounttable to scan.
     */
    public static List<String> determineNamespaceRoot() {
        List<String> result = new ArrayList<>();
        result.add("/" + Config.MT_ADDRESS);
        return result;
    }

    public static String syncName(String id) {
        return NamingUtil.join("/", Config.MT_ADDRESS, id);
    }

    public VContext getVContext() {
        return mBaseContext;
    }

    public void init(Context androidCtx, Activity activity) {
        init(androidCtx, null, activity);
    }

    public synchronized void init(
            Context androidCtx, Blessings otherBlessings, Activity activity) {
        Log.d(TAG, "init");
        if (mAndroidCtx != null) {
            if (mAndroidCtx == androidCtx) {
                Log.d(TAG, "Initialization already started.");
                return;
            } else {
                Log.d(TAG, "Initialization with new context.");
                shutdown(Behavior.STRICT);
            }
        }
        Blessings blessings = otherBlessings;
        mAndroidCtx = androidCtx;
        // Must call V.init before attempting to load blessings, so that proper
        // code is loaded.
        mBaseContext = V.init(mAndroidCtx);
        if (blessings == null) {
            blessings = loadBlessings(androidCtx);
        }
        Namespace ns = V.getNamespace(mBaseContext);
        try {
            ns.setRoots(determineNamespaceRoot());
            Log.d(TAG, "Set namespace root to: " + determineNamespaceRoot());
        } catch (VException e) {
            throw new IllegalStateException("Unable to set namespace.");
        }
        if (blessings == null) {
            Log.d(TAG, "No blessings - firing activity " + activity.getTitle());
            // Bail out and go get them, and re-enter init with them.
            if (activity == null) {
                throw new IllegalArgumentException(
                        "Cannot get blessings without an activity to return to.");
            }
            // Get the signed-in user's email to generate the blessings from.
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(androidCtx);
            String userEmail = prefs.getString(SignInActivity.PREF_USER_ACCOUNT_NAME, "");
            activity.startActivityForResult(
                    BlessingService.newBlessingIntent(androidCtx, userEmail),
                    BLESSING_REQUEST);
            return;
        }
        configurePrincipal(blessings);
        DB.Singleton.get(mAndroidCtx).init();
    }

    public void flushServerFromCache(String name) {
        V.getNamespace(mBaseContext).flushCacheEntry(mBaseContext, name);
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
            VPrincipal p = V.getPrincipal(mBaseContext);
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

    public void shutdown(Behavior behavior) {
        Log.d(TAG, "Shutdown");
        if (mAndroidCtx == null) {
            if (behavior == Behavior.STRICT) {
                throw new IllegalStateException(
                        "Shutdown called on uninitialized manager.");
            }
            Log.d(TAG, "Was never initialized.");
            return;
        }
        V.shutdown();
        mAndroidCtx = null;
    }

    private void error(String msg) {
        Log.e(TAG, msg);
        Toast.makeText(mAndroidCtx, msg, Toast.LENGTH_LONG).show();
    }

    public Set<String> scan(String pattern) {
        FirstGrabber grabber = new FirstGrabber();
        scan(pattern, grabber);
        return grabber.result;
    }

    /**
     * For every server, take the first endpoint, ignore the rest.
     */
    private class FirstGrabber implements Visitor {
        final HashSet<String> result = new HashSet<>();

        public void visit(MountEntry entry) {
            Log.d(TAG, "  Entry: \"" + entry.getName() + "\"");
            result.add(entry.getName());
            final boolean logEndpoints = true;
            if (logEndpoints) {
                for (MountedServer server : entry.getServers()) {
                    Log.d(TAG, "  endPoint: \"" + server.getServer() + "\"");
                }
            }
        }
    }

    public void scan(String pattern, Visitor visitor) {
        try {
            VContext ctx = mBaseContext.withTimeout(MT_TIMEOUT);
            Namespace ns = V.getNamespace(ctx);
            for (GlobReply reply : ns.glob(ctx, pattern)) {
                if (reply instanceof GlobReply.Entry) {
                    visitor.visit(((GlobReply.Entry) reply).getElem());
                }
            }
        } catch (VException e) {
            // TODO(jregan): Handle total v23 failure higher up the stack.
            throw new IllegalStateException(e);
        }
    }

    private VContext getListenContext() throws VException {
        final boolean useProxy = false;
        // Disabled while debugging network performance / visibility issues.
        if (useProxy) {
            ListenSpec spec = V.getListenSpec(mBaseContext).withProxy("proxy");
            //ListenSpec spec = V.getListenSpec(mBaseContext).withAddress(
            //        new ListenSpec.Address("tcp", "0.0.0.0:0"));
            Log.d(TAG, "spec : " + spec.toString());
            Log.d(TAG, "spec proxy: " + spec.getProxy().toString());
            return V.withListenSpec(mBaseContext, spec);
        }
        return mBaseContext;
    }

    private Server makeServer(String mountName, Object server) throws VException {
        return V.getServer(
                V.withNewServer(
                        getListenContext(),
                        mountName,
                        server,
                        VSecurity.newAllowEveryoneAuthorizer()));
    }

    public void mount(final String mountName, final Object server) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "mounting on name \"" + mountName +
                        "\" at table " + Config.MT_ADDRESS);
                try {
                    mLiveServer = makeServer(mountName, server);
                    Log.d(TAG, "  Server status proxies: " +
                            Arrays.deepToString(
                                    mLiveServer.getStatus().getProxies()));
                    Endpoint[] points = mLiveServer.getStatus().getEndpoints();
                    for (Endpoint point : points) {
                        Log.d(TAG, "  Listening on: " + point);
                    }
                    if (points.length < 1) {
                        throw new IllegalStateException("No endpoints!");
                    }
                } catch (VException e) {
                    // TODO(jregan): java gymnastics to propagate exceptions
                    // to a callback instead of throwing over a cliff.
                    throw new IllegalStateException(e);
                }
                Log.d(TAG, "Done mounting on name \"" + mountName + "\"");
            }
        });
    }

    public void unMount() {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "unMount");
                if (mLiveServer == null) {
                    return;
                }
                if (mLiveServer.getStatus().getState() != ServerState.SERVER_ACTIVE) {
                    throw new IllegalStateException("v32 service not active.");
                }
                try {
                    mLiveServer.stop();
                } catch (VException e) {
                    throw new IllegalStateException(e);
                }
                Log.d(TAG, "unMounted server.");
                mLiveServer = null;
            }
        });
    }

    public enum Behavior {PERMISSIVE, STRICT}

    public interface Visitor {
        void visit(MountEntry entry);
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
