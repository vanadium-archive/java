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
import io.v.v23.context.VContext;
import io.v.v23.rpc.Server;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;
import io.v.v23.security.access.Constants;
import io.v.v23.security.access.Permissions;
import io.v.v23.syncbase.SyncbaseService;
import io.v.v23.verror.VException;

/**
 * Syncbase is a storage system for developers that makes it easy to synchronize app data between
 * devices. It works even when devices are not connected to the Internet.
 */
public class Syncbase {
    /**
     * Options for opening a database.
     */
    public static class DatabaseOptions {
        // TODO(sadovsky): Fill this in further.
        public String rootDir;
        // FOR ADVANCED USERS. If true, the user's data will not be synced across their devices.
        public boolean disableUserdata;
        // TODO(sadovsky): Drop this once we switch from io.v.v23.syncbase to io.v.syncbase.core.
        public VAndroidContext vAndroidContext;
    }

    private static DatabaseOptions sOpts;
    private static Database sDatabase;

    // TODO(sadovsky): Maybe select values for DB_NAME and USERDATA_SYNCGROUP_NAME that are less
    // likely to collide with developer-specified names.

    protected static final String
            TAG = "syncbase",
            DIR_NAME = "syncbase",
            DB_NAME = "db",
            USERDATA_SYNCGROUP_NAME = "userdata";

    /**
     * Starts Syncbase if needed; creates default database if needed; performs create-or-join for
     * "userdata" syncgroup if needed; returns database handle.
     *
     * @param opts options for database creation
     * @return the database handle
     */
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
        // FIXME(sadovsky): Add create-or-join of userdata syncgroup, and make this method async.
        // TODO(sadovsky): The create-or-join forces this method to be async, which is annoying
        // since create-or-join will no longer be necessary once syncgroup merge is supported.
        return sDatabase;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // TODO(sadovsky): Remove much of the code below once we switch from io.v.v23.syncbase to
    // io.v.syncbase.core. Note, much of this code was copied from the Todos app.

    protected static VContext getVContext() {
        return sOpts.vAndroidContext.getVContext();
    }

    // TODO(sadovsky): Some of these constants should become fields in DatabaseOptions.
    protected static final String
            PROXY = "proxy",
            DEFAULT_BLESSING_STRING_PREFIX = "dev.v.io:o:608941808256-43vtfndets79kf5hac8ieujto8837660.apps.googleusercontent.com:",
            MOUNT_POINT = "/ns.dev.v.io:8101/tmp/todos/users/",
            CLOUD_BLESSING_STRING = "dev.v.io:u:alexfandrianto@google.com",
            CLOUD_NAME = MOUNT_POINT + "cloud";

    private static Database startSyncbaseAndInitDatabase(VContext ctx) {
        SyncbaseService s;
        try {
            s = io.v.v23.syncbase.Syncbase.newService(startSyncbase(ctx, sOpts.rootDir));
        } catch (SyncbaseServer.StartException e) {
            throw new RuntimeException("Failed to start Syncbase", e);
        }
        // Create database, if needed.
        Database res = new Database(s.getDatabase(getVContext(), DB_NAME, null));
        res.createIfMissing();
        return res;
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

    protected static String getEmailFromBlessingPattern(BlessingPattern pattern) {
        return getEmailFromBlessingString(pattern.toString());
    }

    private static String getEmailFromBlessingString(String blessingStr) {
        String[] parts = blessingStr.split(":");
        return parts[parts.length - 1];
    }

    private static String getBlessingStringFromEmail(String email) {
        return DEFAULT_BLESSING_STRING_PREFIX + email;
    }

    protected static BlessingPattern getBlessingPatternFromEmail(String email) {
        return new BlessingPattern(getBlessingStringFromEmail(email));
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

    protected static Permissions defaultPerms() {
        // TODO(sadovsky): Revisit these default perms, which were copied from the Todos app.
        io.v.v23.security.access.AccessList anyone =
                new io.v.v23.security.access.AccessList(
                        ImmutableList.of(
                                new BlessingPattern("...")),
                        ImmutableList.<String>of());
        io.v.v23.security.access.AccessList selfAndCloud =
                new io.v.v23.security.access.AccessList(
                        ImmutableList.of(
                                new BlessingPattern(getPersonalBlessingString()),
                                new BlessingPattern(CLOUD_BLESSING_STRING)),
                        ImmutableList.<String>of());
        return new Permissions(ImmutableMap.of(
                Constants.RESOLVE.getValue(), anyone,
                Constants.READ.getValue(), selfAndCloud,
                Constants.WRITE.getValue(), selfAndCloud,
                Constants.ADMIN.getValue(), selfAndCloud));
    }
}