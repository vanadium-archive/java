// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.v.syncbase.core.Permissions;
import io.v.syncbase.core.Service;
import io.v.syncbase.core.VError;

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
    public static class Options {
        // Where data should be persisted.
        public String rootDir;
        // We use an empty mountPoints to avoid talking to the global mounttabled.
        public List<String> mountPoints = new ArrayList<>();
        // TODO(sadovsky): Figure out how developers should specify this.
        public String adminUserId = "alexfandrianto@google.com";
        // TODO(sadovsky): Figure out how developers should specify this.
        public String defaultBlessingStringPrefix = "dev.v.io:o:608941808256-43vtfndets79kf5hac8ieujto8837660.apps.googleusercontent.com:";
        // FOR ADVANCED USERS. If true, syncgroups will not be published to the cloud peer.
        public boolean disableSyncgroupPublishing;
        // FOR ADVANCED USERS. If true, the user's data will not be synced across their devices.
        public boolean disableUserdataSyncgroup;
        // FOR TESTING. If true, the app name is set to 'app', the user name is set to 'user' and
        // the arguments to login() are ignored.
        public boolean testLogin;

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

    protected static Options sOpts;
    private static Database sDatabase;
    private static Map sSelfAndCloud;

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

    /**
     * Sets the initial options. If the user is already logged in, Syncbase will be started.
     *
     * @param opts
     */
    public static void init(Options opts) throws VError {
        sOpts = opts;
        sSelfAndCloud = ImmutableMap.of(
                Permissions.IN, ImmutableList.of(getPersonalBlessingString(),
                        sOpts.getCloudBlessingString()));
        io.v.syncbase.internal.Service.Init(sOpts.rootDir, sOpts.testLogin);
        if (isLoggedIn()) {
            io.v.syncbase.internal.Service.Serve();
        }
    }

    /**
     * Returns a Database object. Return null if the user is not currently logged in.
     */
    public static Database database() throws VError {
        if (!isLoggedIn()) {
            return null;
        }
        if (sDatabase != null) {
            // TODO(sadovsky): Check that opts matches original opts (sOpts)?
            return sDatabase;
        }
        // TODO(razvanm): Use just the name after Blessings.AppBlessingFromContext starts
        // working.
        sDatabase = new Database(Service.database(new io.v.syncbase.core.Id("...", DB_NAME)));
        return sDatabase;
    }

    /**
     * Close the database and stop Syncbase.
     */
    public static void shutdown() {
        io.v.syncbase.internal.Service.Shutdown();
        sDatabase = null;
    }

    /**
     * Returns true iff the user is currently logged in.
     */
    public static boolean isLoggedIn() {
        return io.v.syncbase.internal.Service.IsLoggedIn();
    }

    /**
     * Returns the currently logged in user.
     */
    public static User getLoggedInUser() {
        throw new RuntimeException("Not implemented");
    }

    public static abstract class LoginCallback {
        public void onSuccess() {
        }

        public void onError(Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Logs in the user associated with the given OAuth token and provider and starts Syncbase;
     * creates default database if needed; performs create-or-join for "userdata" syncgroup if
     * needed. The passed callback is called on the current thread.
     *
     * <p/>
     * A mapping of providers and OAuth token scopes are listed below:
     * google: https://www.googleapis.com/auth/userinfo.email
     * <p/>
     * Note: Unlisted providers are unsupported.
     *
     * @param authToken The OAuth token for the user to be logged in.
     * @param provider  The provider of the OAuth token.
     * @param cb        The callback to call when the login was done.
     */
    public static void login(final String authToken, final String provider, final LoginCallback cb) {
        if (!(provider.equals(User.PROVIDER_GOOGLE) || provider.equals(User.PROVIDER_NONE))) {
            throw new RuntimeException("Unsupported provider: " + provider);
        }

        Syncbase.enqueue(new Runnable() {
            @Override
            public void run() {
                try {
                    io.v.syncbase.internal.Service.Login(provider, authToken);
                    sDatabase = database();
                    sDatabase.createIfMissing();
                    if (sOpts.disableUserdataSyncgroup) {
                        Database.CollectionOptions cxOpts = new DatabaseHandle.CollectionOptions();
                        cxOpts.withoutSyncgroup = true;
                        sDatabase.collection(USERDATA_SYNCGROUP_NAME, cxOpts);
                        Syncbase.enqueue(new Runnable() {
                            @Override
                            public void run() {
                                cb.onSuccess();
                            }
                        });
                    } else {
                        // FIXME(sadovsky): Implement create-or-join (and watch) of userdata
                        // syncgroup. For the new JNI API, we'll need to add Go code for this,
                        // since Java can't make RPCs.
                        cb.onError(new RuntimeException(
                                "Synced userdata collection is not yet supported"));
                    }
                } catch (VError vError) {
                    cb.onError(vError);
                }
            }
        });
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

    protected static String getBlessingStringFromEmail(String email) {
        return sOpts.defaultBlessingStringPrefix + email;
    }

    protected static String getEmailFromBlessingPattern(String blessingStr) {
        String[] parts = blessingStr.split(":");
        return parts[parts.length - 1];
    }

    protected static String getPersonalBlessingString() {
        // TODO(razvanm): Switch to Blessings.UserBlessingFromContext() after the lower level
        // starts working.
        return "...";
    }

    protected static Permissions defaultDatabasePerms() throws VError {
        // TODO(sadovsky): Revisit these default perms, which were copied from the Todos app.
        Map anyone = ImmutableMap.of(Permissions.IN, ImmutableList.of("..."));
        return new Permissions(ImmutableMap.of(
                Permissions.Tags.RESOLVE, anyone,
                Permissions.Tags.READ, sSelfAndCloud,
                Permissions.Tags.WRITE, sSelfAndCloud,
                Permissions.Tags.ADMIN, sSelfAndCloud));
    }

    protected static Permissions defaultCollectionPerms() throws VError {
        // TODO(sadovsky): Revisit these default perms, which were copied from the Todos app.
        return new Permissions(ImmutableMap.of(
                Permissions.Tags.READ, sSelfAndCloud,
                Permissions.Tags.WRITE, sSelfAndCloud,
                Permissions.Tags.ADMIN, sSelfAndCloud));
    }

    protected static Permissions defaultSyncgroupPerms() throws VError {
        // TODO(sadovsky): Revisit these default perms, which were copied from the Todos app.
        return new Permissions(ImmutableMap.of(
                Permissions.Tags.READ, sSelfAndCloud,
                Permissions.Tags.ADMIN, sSelfAndCloud));
    }
}