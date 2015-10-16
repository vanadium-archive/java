// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import android.content.Context;
import android.util.Log;

import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.v.android.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.namespace.Namespace;
import io.v.v23.naming.Endpoint;
import io.v.v23.naming.GlobReply;
import io.v.v23.naming.MountEntry;
import io.v.v23.naming.MountedServer;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerState;
import io.v.v23.security.VSecurity;
import io.v.v23.verror.VException;

/**
 * Does vanadium stuff - MT scanning, service creation, unmounting, etc.
 *
 * This class is a singleton, since all vanadium activity must involve a
 * Vanadium context recovered from a static call to V.init, ultimately (ideally)
 * bookended by a static call to V.shutdown.   In an app.Service, one could call
 * these in onCreate and onDestroy respectively.
 */
public class V23Manager {
    private static final String TAG = "V23Manager";

    private static final Duration MT_TIMEOUT =
            Duration.standardSeconds(5);
    // Generates a name to use in the MT.
    private final NameGenerator mNameGenerator = new NameGeneratorByDate();
    private Context mAndroidCtx;
    private VContext mBaseContext = null;
    private VContext mMTContext = null;
    private Namespace mNamespace = null;
    // Can only have one of these at the moment.  Could add more...
    private Server mLiveServer = null;

    // Singleton.
    private V23Manager() {
    }

    /**
     * Placeholder for possibly scraping a website for the NS Root.
     *
     * @return IP address of the mounttable to scan.
     */
    private static List<String> determineNamespaceRoot() {
        List<String> result = new ArrayList<>();
        result.add("/" + FixedMt.PI_MILK_CRATE);
        return result;
    }

    public Context getAndroidCtx() {
        return mAndroidCtx;
    }

    public void init(Context androidCtx) {
        Log.d(TAG, "init");
        if (mAndroidCtx != null) {
            if (mAndroidCtx == androidCtx) {
                Log.d(TAG, "Already initialized.");
                return;
            } else {
                shutdown(Behavior.STRICT);
            }
        }
        mAndroidCtx = androidCtx;
        mBaseContext = V.init(mAndroidCtx);
        mMTContext = mBaseContext.withTimeout(MT_TIMEOUT);
        mNamespace = V.getNamespace(mMTContext);
        try {
            mNamespace.setRoots(determineNamespaceRoot());
        } catch (VException e) {
            // TODO(jregan): Handle total v23 failure higher up the stack.
            throw new IllegalStateException(e);
        }
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

    public Set<String> scan(String pattern) {
        FirstGrabber grabber = new FirstGrabber();
        scan(pattern, grabber);
        return grabber.result;
    }

    public void scan(String pattern, Visitor visitor) {
        try {
            for (GlobReply reply : mNamespace.glob(mMTContext, pattern)) {
                if (reply instanceof GlobReply.Entry) {
                    visitor.visit(((GlobReply.Entry) reply).getElem());
                }
            }
        } catch (VException e) {
            // TODO(jregan): Handle total v23 failure higher up the stack.
            throw new IllegalStateException(e);
        }
    }

    public String mount(String mountName, Object server) {
        Log.d(TAG, "mount");
        try {
            // ListenSpec spec = V.getListenSpec(mBaseContext).withProxy("proxy");
            ListenSpec spec = V.getListenSpec(mBaseContext).withAddress(
                    new ListenSpec.Address("tcp", "localhost:0"));
            VContext ctx = V.withNewServer(
                    V.withListenSpec(mBaseContext, spec),
                    mountName,
                    server,
                    VSecurity.newAllowEveryoneAuthorizer());
            mLiveServer = V.getServer(ctx);
            Log.d(TAG, "Server status: " + mLiveServer.getStatus().getState());
            Endpoint[] endpoints = mLiveServer.getStatus().getEndpoints();
            Log.d(TAG, "Listening on endpoints: " + Arrays.toString(endpoints));
            if (endpoints.length < 1) {
                throw new IllegalStateException("No endpoints!");
            }
            return endpoints[0].name();
        } catch (VException e) {
            // TODO(jregan): Handle total v23 failure higher up the stack.
            throw new IllegalStateException(e);
        }
    }

    public void unMount() {
        Log.d(TAG, "unMount");
        if (mLiveServer == null) {
            throw new IllegalStateException("No v32 service");
        }
        if (mLiveServer.getStatus().getState() != ServerState.SERVER_ACTIVE) {
            throw new IllegalStateException("v32 service not active.");
        }
        try {
            mLiveServer.stop();
        } catch (VException e) {
            throw new IllegalStateException(e);
        }
        mLiveServer = null;
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

    /**
     * Some fixed mount tables to try.
     */
    private static class FixedMt {
        static final String PI_MILK_CRATE = "192.168.86.254:8101";
        static final String JR_LAPTOP_AT_HOME = "192.168.2.71:23000";
        static final String JR_LAPTOP_VEYRON = "192.168.8.106:23000";
    }

    /**
     * For every server, take the first endpoint, ignore the rest.
     */
    private class FirstGrabber implements Visitor {
        final HashSet<String> result = new HashSet<>();

        public void visit(MountEntry entry) {
            for (MountedServer server : entry.getServers()) {
                String endPoint = server.getServer();
                result.add(endPoint);
                return;
            }
        }
    }
}
