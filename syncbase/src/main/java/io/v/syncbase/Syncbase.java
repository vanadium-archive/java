// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.v.impl.google.services.syncbase.SyncbaseServer;
import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Server;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;
import io.v.v23.security.access.Constants;
import io.v.v23.security.access.Permissions;
import io.v.v23.syncbase.SyncbaseService;
import io.v.v23.verror.VException;

// FIXME(sadovsky): Currently, various methods throw RuntimeException on any error. We need to
// decide which error types to surface to clients, and define specific Exception subclasses for
// those.

/**
 * The "userdata" collection is a per-user collection (and associated syncgroup) for data that
 * should automatically get synced across a given user's devices. It has the following schema:
 * - /syncgroups/{encodedSyncgroupId} -> null
 * - /ignoredInvites/{encodedSyncgroupId} -> null
 */

/**
 * Syncbase is a storage system for developers that makes it easy to synchronize app data between
 * devices. It works even when devices are not connected to the Internet.
 */
public class Syncbase {
    /**
     * Options for opening a database.
     */
    public static class DatabaseOptions {
        // Where data should be persisted.
        public String rootDir;
        // TODO(sadovsky): Figure out what this should default to.
        public List<String> mountPoints = ImmutableList.of("/ns.dev.v.io:8101/tmp/todos/users/");
        // TODO(sadovsky): Figure out how developers should specify this.
        public String adminUserId = "alexfandrianto@google.com";
        // TODO(sadovsky): Figure out how developers should specify this.
        public String defaultBlessingStringPrefix = "dev.v.io:o:608941808256-43vtfndets79kf5hac8ieujto8837660.apps.googleusercontent.com:";
        // FOR ADVANCED USERS. If true, syncgroups will not be published to the cloud peer.
        public boolean disableSyncgroupPublishing;
        // FOR ADVANCED USERS. If true, the user's data will not be synced across their devices.
        public boolean disableUserdataSyncgroup;
        // TODO(sadovsky): Drop this once we switch from io.v.v23.syncbase to io.v.syncbase.core.
        public VContext vContext;

        protected String getPublishSyncbaseName() {
            if (disableSyncgroupPublishing) {
                return null;
            }
            return mountPoints.get(0) + "cloud";
        }

        protected String getCloudBlessingString() {
            return "dev.v.io:u:" + adminUserId;
        }
    }

    protected static DatabaseOptions sOpts;
    private static Database sDatabase;

    // TODO(sadovsky): Maybe set DB_NAME to "db__" so that it is less likely to collide with
    // developer-specified names.

    protected static final String
            TAG = "syncbase",
            DIR_NAME = "syncbase",
            DB_NAME = "db",
            USERDATA_SYNCGROUP_NAME = "userdata__";

    protected static void enqueue(final Runnable r) {
        // Note, we use Timer rather than Handler because the latter must be mocked out for tests,
        // which is rather annoying.
        //new Handler().post(r);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                r.run();
            }
        }, 0);
    }

    public static abstract class DatabaseCallback {
        public void onSuccess(Database db) {
        }

        public void onError(Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Starts Syncbase if needed; creates default database if needed; performs create-or-join for
     * "userdata" syncgroup if needed. The passed callback is called on the current thread.
     * Fails if the user is not logged in.
     *
     * @param opts options for database creation
     * @param cb   the callback to call with the database handle
     */
    public static void database(final DatabaseCallback cb, DatabaseOptions opts) {
        // TODO(sadovsky): The create-or-join forces this method to be async, which is annoying
        // since create-or-join will no longer be necessary once syncgroup merge is supported.
        if (sDatabase != null) {
            // TODO(sadovsky): Check that opts matches original opts (sOpts)?
            Syncbase.enqueue(new Runnable() {
                @Override
                public void run() {
                    cb.onSuccess(sDatabase);
                }
            });
            return;
        }
        sOpts = opts;
        // TODO(sadovsky): Call ctx.cancel in sDatabase destructor?
        VContext ctx = getVContext().withCancel();
        try {
            sDatabase = startSyncbaseAndInitDatabase(ctx);
        } catch (final Exception e) {
            ctx.cancel();
            Syncbase.enqueue(new Runnable() {
                @Override
                public void run() {
                    cb.onError(e);
                }
            });
            return;
        }
        if (sOpts.disableUserdataSyncgroup) {
            Database.CollectionOptions cxOpts = new DatabaseHandle.CollectionOptions();
            cxOpts.withoutSyncgroup = true;
            sDatabase.collection(USERDATA_SYNCGROUP_NAME, cxOpts);
            Syncbase.enqueue(new Runnable() {
                @Override
                public void run() {
                    cb.onSuccess(sDatabase);
                }
            });
        } else {
            // FIXME(sadovsky): Implement create-or-join (and watch) of userdata syncgroup. For the
            // new JNI API, we'll need to add Go code for this, since Java can't make RPCs.
            cb.onError(new RuntimeException("Synced userdata collection is not yet supported"));
        }
    }

    /**
     * Returns true iff the user is currently logged in.
     */
    public static boolean isLoggedIn() {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Returns the currently logged in user.
     */
    public static User getLoggedInUser() {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Logs in the user associated with the given OAuth token and provider.
     *
     * A mapping of providers and OAuth token scopes are listed below:
     * google: https://www.googleapis.com/auth/userinfo.email
     *
     * Note: Unlisted providers are unsupported.
     *
     * @param authToken The OAuth token for the user to be logged in.
     * @param provider  The provider of the OAuth token.
     */
    public static void login(String authToken, String provider) {
        if (!provider.equals("google")) {
            throw new RuntimeException("Unsupported provider: " + provider);
        }
        throw new RuntimeException("Not implemented");
    }

    /**
     * Scans the neighborhood for nearby users.
     *
     * @param cb The callback to call when a User is found or lost.
     */
    public static void scanNeighborhoodForUsers(ScanNeighborhoodForUsersCallback cb) {
        throw new RuntimeException("Not implemented");
    }

    public static abstract class ScanNeighborhoodForUsersCallback {
        public abstract void onFound(User user);
        public abstract void onLost(User user);
        public void onError(Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Advertises the logged in user's presence to those around them.
     */
    public static void advertiseLoggedInUserInNeighborhood() {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Advertises the logged in user's presence to a limited set of users who must be around them.
     *
     * @param usersWhoCanSee The set of users who are allowed to find this user.
     */
    public static void advertiseLoggedInUserInNeighborhood(Iterable<User> usersWhoCanSee) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Stops advertising the presence of the logged in user so that they can no longer be found.
     */
    public static void stopAdvertisingLoggedInUserInNeighborhood() {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Returns true iff this person appears in the neighborhood.
     */
    public static boolean isAdvertisingLoggedInUserInNeighborhood() {
        throw new RuntimeException("Not implemented");
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // TODO(sadovsky): Remove much of the code below once we switch from io.v.v23.syncbase to
    // io.v.syncbase.core. Note, much of this code was copied from the Todos app.

    protected static VContext getVContext() {
        return sOpts.vContext;
    }

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
            // TODO(sadovsky): Make proxy configurable?
            vContext = V.withListenSpec(vContext, V.getListenSpec(vContext).withProxy("proxy"));
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
        return sOpts.defaultBlessingStringPrefix + email;
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
                                new BlessingPattern(sOpts.getCloudBlessingString())),
                        ImmutableList.<String>of());
        return new Permissions(ImmutableMap.of(
                Constants.RESOLVE.getValue(), anyone,
                Constants.READ.getValue(), selfAndCloud,
                Constants.WRITE.getValue(), selfAndCloud,
                Constants.ADMIN.getValue(), selfAndCloud));
    }
}