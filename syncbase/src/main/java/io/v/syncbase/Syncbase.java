// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.File;

import io.v.android.VAndroidContext;
import io.v.android.v23.V;
import io.v.impl.google.services.syncbase.SyncbaseServer;
import io.v.v23.VFutures;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Server;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;
import io.v.v23.security.access.Constants;
import io.v.v23.security.access.Permissions;
import io.v.v23.syncbase.SyncbaseService;
import io.v.v23.verror.ExistException;
import io.v.v23.verror.VException;

public class Syncbase {
    public static class DatabaseOptions {
        // TODO(sadovsky): Fill this in further.
        public String rootDir;
        // TODO(sadovsky): Drop this once we switch from io.v.v23.syncbase to io.v.syncbase.core.
        public VAndroidContext vAndroidContext;
    }

    private static DatabaseOptions sOpts;
    private static Database sDatabase;

    private static final String
            TAG = "syncbase",
            DIR_NAME = "syncbase",
            DB_NAME = "db";

    // Starts Syncbase if needed; creates default database if needed; reads config (e.g. cloud
    // syncbase name) from options struct; performs create-or-join for "userdata" syncgroup if
    // needed; returns database handle.
    // TODO(sadovsky): The create-or-join forces this method to be async, which is annoying since
    // create-or-join will no longer be necessary once syncgroup merge is supported.
    public static Database database(DatabaseOptions opts) {
        if (sDatabase != null) {
            // TODO(sadovsky): Check that opts matches original opts (sOpts)?
            return sDatabase;
        }
        sOpts = opts;
        // TODO(sadovsky): Call ctx.cancel in sDatabase destructor?
        VContext ctx = getVContext().withCancel();
        try {
            sDatabase = startSyncbaseAndInitDatabase(ctx);
        } catch (Exception e) {
            ctx.cancel();
            throw e;
        }
        // TODO(sadovsky): Add create-or-join of userdata syncgroup, and make this method async.
        return sDatabase;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // TODO(sadovsky): Remove much of the code below once we switch from io.v.v23.syncbase to
    // io.v.syncbase.core. Note, much of this code was copied from the Todos app.

    protected static VContext getVContext() {
        return sOpts.vAndroidContext.getVContext();
    }

    // TODO(sadovsky): Some of these constants should become fields in DatabaseOptions.
    private static final String
            PROXY = "proxy",
            DEFAULT_BLESSING_STRING_PREFIX = "dev.v.io:o:608941808256-43vtfndets79kf5hac8ieujto8837660.apps.googleusercontent.com:",
            MOUNT_POINT = "/ns.dev.v.io:8101/tmp/todos/users/",
            CLOUD_BLESSING = "dev.v.io:u:alexfandrianto@google.com";

    private static Database startSyncbaseAndInitDatabase(VContext ctx) {
        SyncbaseService s;
        Blessings personalBlessings = getPersonalBlessings();
        if (personalBlessings == null) {
            throw new RuntimeException("No blessings");
        }
        Permissions perms = permissionsFromBlessings(personalBlessings);
        try {
            s = io.v.v23.syncbase.Syncbase.newService(startSyncbase(ctx, sOpts.rootDir));
        } catch (SyncbaseServer.StartException e) {
            throw new RuntimeException("Failed to start Syncbase", e);
        }
        // Create database, if needed.
        io.v.v23.syncbase.Database d = s.getDatabase(getVContext(), DB_NAME, null);
        try {
            VFutures.sync(d.create(getVContext(), perms));
        } catch (ExistException e) {
            // Database already exists, presumably from a previous run of the app.
        } catch (VException e) {
            throw new RuntimeException("Failed to create database", e);
        }
        return new Database(d);
    }

    private static String startSyncbase(VContext vContext, String rootDir)
            throws SyncbaseServer.StartException {
        try {
            vContext = V.withListenSpec(vContext, V.getListenSpec(vContext).withProxy(PROXY));
        } catch (VException e) {
            Log.w(TAG, "Failed to set up Vanadium proxy", e);
        }
        File dir = new File(rootDir, DIR_NAME);
        dir.mkdirs();
        SyncbaseServer.Params params = new SyncbaseServer.Params()
                .withStorageRootDir(dir.getAbsolutePath());
        VContext serverContext = SyncbaseServer.withNewServer(vContext, params);
        Server server = V.getServer(serverContext);
        String name = server.getStatus().getEndpoints()[0].name();
        Log.i(TAG, "Started Syncbase: " + name);
        return name;
    }

    private static void checkHasOneBlessing(Blessings blessings) {
        int n = blessings.getCertificateChains().size();
        if (n != 1) {
            throw new RuntimeException("Expected one blessing, got " + n);
        }
    }

    private static String getEmailFromBlessings(Blessings blessings) {
        checkHasOneBlessing(blessings);
        return getEmailFromBlessingString(blessings.toString());
    }

    private static String getEmailFromBlessingPattern(BlessingPattern pattern) {
        return getEmailFromBlessingString(pattern.toString());
    }

    private static String getEmailFromBlessingString(String blessingStr) {
        String[] parts = blessingStr.split(":");
        return parts[parts.length - 1];
    }

    private static String getBlessingStringFromEmail(String email) {
        return DEFAULT_BLESSING_STRING_PREFIX + email;
    }

    private static Blessings getPersonalBlessings() {
        return V.getPrincipal(getVContext()).blessingStore().defaultBlessings();
    }

    protected static String getPersonalBlessingString() {
        Blessings blessings = getPersonalBlessings();
        checkHasOneBlessing(blessings);
        return blessings.toString();
    }

    private static String getPersonalEmail() {
        return getEmailFromBlessings(getPersonalBlessings());
    }

    protected static Permissions permissionsFromBlessings(Blessings blessings) {
        // TODO(sadovsky): Revisit these default perms, which were copied from the Todos app.
        io.v.v23.security.access.AccessList openAccessList =
                new io.v.v23.security.access.AccessList(
                        ImmutableList.of(
                                new BlessingPattern("...")),
                        ImmutableList.<String>of());
        io.v.v23.security.access.AccessList accessList =
                new io.v.v23.security.access.AccessList(
                        ImmutableList.of(
                                new BlessingPattern(blessings.toString()),
                                new BlessingPattern(CLOUD_BLESSING)),
                        ImmutableList.<String>of());
        return new Permissions(ImmutableMap.of(
                Constants.RESOLVE.getValue(), openAccessList,
                Constants.READ.getValue(), accessList,
                Constants.WRITE.getValue(), accessList,
                Constants.ADMIN.getValue(), accessList));
    }
}