// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.v.syncbase.core.NeighborhoodPeer;
import io.v.syncbase.core.Permissions;
import io.v.syncbase.core.Service;
import io.v.syncbase.core.VError;
import io.v.syncbase.internal.Blessings;
import io.v.syncbase.internal.Neighborhood;

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

        String getPublishSyncbaseName() {
            if (disableSyncgroupPublishing) {
                return null;
            }
            return mountPoints.get(0) + "cloud";
        }

        String getCloudBlessingString() {
            return "dev.v.io:u:" + adminUserId;
        }
    }

    static Options sOpts;
    private static Database sDatabase;
    static Collection sUserdataCollection;
    private static final Object sScanMappingMu = new Object();
    private static final Map<ScanNeighborhoodForUsersCallback, Long> sScanMapping = new HashMap<>();

    // TODO(sadovsky): Maybe set DB_NAME to "db__" so that it is less likely to collide with
    // developer-specified names.

    static final String
            TAG = "syncbase",
            DIR_NAME = "syncbase",
            DB_NAME = "db",
            USERDATA_SYNCGROUP_NAME = "userdata__";

    private static void enqueue(final Runnable r) {
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

    private static Map selfAndCloud() throws VError {
        return ImmutableMap.of(Permissions.IN,
                ImmutableList.of(getPersonalBlessingString(), sOpts.getCloudBlessingString()));
    }

    /**
     * Sets the initial options. If the user is already logged in, Syncbase will be started.
     *
     * @param opts initial options
     */
    public static void init(Options opts) throws VError {
        System.loadLibrary("syncbase");
        sOpts = opts;
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
        sDatabase = new Database(Service.database(DB_NAME));
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
        throw new UnsupportedOperationException("Not implemented");
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
     * @throws IllegalArgumentException if provider is not one of those listed above
     */
    public static void login(final String authToken, final String provider, final LoginCallback cb) {
        if (!(provider.equals(User.PROVIDER_GOOGLE) || provider.equals(User.PROVIDER_NONE))) {
            throw new IllegalArgumentException("Unsupported provider: " + provider);
        }

        Syncbase.enqueue(new Runnable() {
            @Override
            public void run() {
                try {
                    io.v.syncbase.internal.Service.Login(provider, authToken);
                    sDatabase = database();
                    if (sDatabase==null) {
                        cb.onError(new IllegalStateException("not logged in"));
                        return;
                    }
                    sDatabase.createIfMissing();
                    sUserdataCollection = sDatabase.collection(
                            USERDATA_SYNCGROUP_NAME,
                            new DatabaseHandle.CollectionOptions().setWithoutSyncgroup(true));
                    if (!sOpts.disableUserdataSyncgroup) {
                        Syncgroup syncgroup = sUserdataCollection.getSyncgroup();
                        // TODO(razvanm): First we need to try to join, then we need to try to
                        // create the syncgroup.
                        syncgroup.createIfMissing(ImmutableList.of(sUserdataCollection));
                        sDatabase.addWatchChangeHandler(new UserdataWatchHandler());
                    }
                    Syncbase.enqueue(new Runnable() {
                            @Override
                            public void run() {
                                cb.onSuccess();
                            }
                        });
                } catch (VError vError) {
                    cb.onError(vError);
                }
            }
        });
    }

    private static class UserdataWatchHandler extends Database.WatchChangeHandler {
        @Override
        public void onInitialState(Iterator<WatchChange> values) {
            onWatchChange(values);
        }

        @Override
        public void onChangeBatch(Iterator<WatchChange> changes) {
            onWatchChange(changes);
        }

        private void onWatchChange(Iterator<WatchChange> changes) {
            WatchChange watchChange = changes.next();
            if (watchChange.getCollectionId().getName().equals(USERDATA_SYNCGROUP_NAME) &&
                    watchChange.getEntityType() == WatchChange.EntityType.ROW &&
                    watchChange.getChangeType() == WatchChange.ChangeType.PUT) {
                try {
                    sDatabase.getSyncgroup(Id.decode(watchChange.getRowKey())).join();
                } catch (VError vError) {
                    vError.printStackTrace();
                    System.err.println(vError.toString());
                }
            }
        }
    }

    /**
     * Scans the neighborhood for nearby users.
     *
     * @param cb The callback to call when a User is found or lost.
     */
    public static void addScanForUsersInNeighborhood(final ScanNeighborhoodForUsersCallback cb) {
        synchronized (sScanMappingMu) {
            try {
                long scanId = Neighborhood.NewScan(new Neighborhood.NeighborhoodScanCallbacks() {
                    @Override
                    public void onPeer(NeighborhoodPeer peer) {
                        if (peer.isLost) {
                            cb.onLost(new User(getAliasFromBlessingPattern(peer.blessings)));
                        } else {
                            cb.onFound(new User(getAliasFromBlessingPattern(peer.blessings)));
                        }
                    }
                });
                sScanMapping.put(cb, scanId);
            } catch (VError vError) {
                cb.onError(vError);
            }
        }
    }

    /**
     * Removes this callback from receiving new neighborhood scan updates.
     *
     * @param cb The original callback passed to a started scan.
     */
    public static void removeScanForUsersInNeighborhood(ScanNeighborhoodForUsersCallback cb) {
        synchronized (sScanMappingMu) {
            Long scanId = sScanMapping.remove(cb);
            if (scanId != null) {
                Neighborhood.StopScan(scanId);
            }
        }
    }

    /**
     * Stops all existing scanning callbacks from receiving new neighborhood scan updates.
     */
    public static void removeAllScansForUsersInNeighborhood() {
        synchronized (sScanMappingMu) {
            for (Long scanId : sScanMapping.values()) {
                Neighborhood.StopScan(scanId);
            }
            sScanMapping.clear();
        }
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
    public static void advertiseLoggedInUserInNeighborhood() throws VError {
        Neighborhood.StartAdvertising(new ArrayList<String>());
    }

    /**
     * Advertises the logged in user's presence to a limited set of users who must be around them.
     *
     * @param usersWhoCanSee The set of users who are allowed to find this user.
     */
    public static void advertiseLoggedInUserInNeighborhood(Iterable<User> usersWhoCanSee) throws VError {
        List<String> visibility = new ArrayList<String>();
        for (User user : usersWhoCanSee) {
            visibility.add(Syncbase.getBlessingStringFromAlias(user.getAlias()));
        }
        Neighborhood.StartAdvertising(visibility);
    }

    /**
     * Stops advertising the presence of the logged in user so that they can no longer be found.
     */
    public static void stopAdvertisingLoggedInUserInNeighborhood() {
        Neighborhood.StopAdvertising();
    }

    /**
     * Returns true iff this person appears in the neighborhood.
     */
    public static boolean isAdvertisingLoggedInUserInNeighborhood() {
        return Neighborhood.IsAdvertising();
    }

    protected static String getBlessingStringFromAlias(String alias) {
        return sOpts.defaultBlessingStringPrefix + alias;
    }

    protected static String getAliasFromBlessingPattern(String blessingStr) {
        String[] parts = blessingStr.split(":");
        return parts[parts.length - 1];
    }

    static String getPersonalBlessingString() throws VError {
        return Blessings.UserBlessingFromContext();
    }

    static Permissions defaultDatabasePerms() throws VError {
        // TODO(sadovsky): Revisit these default perms, which were copied from the Todos app.
        Map anyone = ImmutableMap.of(Permissions.IN, ImmutableList.of("..."));
        Map selfAndCloud = selfAndCloud();
        return new Permissions(ImmutableMap.of(
                Permissions.Tags.RESOLVE, anyone,
                Permissions.Tags.READ, selfAndCloud,
                Permissions.Tags.WRITE, selfAndCloud,
                Permissions.Tags.ADMIN, selfAndCloud));
    }

    static Permissions defaultCollectionPerms() throws VError {
        // TODO(sadovsky): Revisit these default perms, which were copied from the Todos app.
        Map selfAndCloud = selfAndCloud();
        return new Permissions(ImmutableMap.of(
                Permissions.Tags.READ, selfAndCloud,
                Permissions.Tags.WRITE, selfAndCloud,
                Permissions.Tags.ADMIN, selfAndCloud));
    }

    static Permissions defaultSyncgroupPerms() throws VError {
        // TODO(sadovsky): Revisit these default perms, which were copied from the Todos app.
        Map selfAndCloud = selfAndCloud();
        return new Permissions(ImmutableMap.of(
                Permissions.Tags.READ, selfAndCloud,
                Permissions.Tags.ADMIN, selfAndCloud));
    }
}