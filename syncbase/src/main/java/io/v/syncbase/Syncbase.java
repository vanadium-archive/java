// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Handler;
import android.os.Looper;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.SettableFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import io.v.syncbase.android.LoginFragment;
import io.v.syncbase.core.NeighborhoodPeer;
import io.v.syncbase.core.Permissions;
import io.v.syncbase.core.Service;
import io.v.syncbase.core.VError;
import io.v.syncbase.exception.SyncbaseException;
import io.v.syncbase.internal.Blessings;
import io.v.syncbase.internal.Neighborhood;

import static io.v.syncbase.exception.Exceptions.chainThrow;

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
 *
 * <p>Methods of classes in this package may throw an exception that is a subclass of
 * SyncbaseException.  See details of those subclasses to determine whether there are conditions
 * the calling code should handle.</p>
 */
public class Syncbase {
    /**
     * Options for opening a database.
     */
    public static class Options {
        // The executor used to execute callbacks.
        public Executor callbackExecutor = UiThreadExecutor.INSTANCE;
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

    /**
     * Executor that executes all of its commands on the Android UI thread.
     */
    private static class UiThreadExecutor implements Executor {
        /**
         * Singleton instance of the UiThreadExecutor.
         */
        public static final UiThreadExecutor INSTANCE = new UiThreadExecutor();

        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable runnable) {
            handler.post(runnable);
        }
        private UiThreadExecutor() {}
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
            DB_NAME = "db";

    public static final String
            USERDATA_NAME = "userdata__",
            USERDATA_COLLECTION_PREFIX = "__collections/";

    private static Map selfAndCloud() throws SyncbaseException {
        return ImmutableMap.of(Permissions.IN,
                ImmutableList.of(getPersonalBlessingString(), sOpts.getCloudBlessingString()));
    }

    /**
     * Sets the initial options. If the user is already logged in, Syncbase will be started.
     *
     * @param opts initial options
     */
    public static void init(Options opts) throws SyncbaseException {
        try {

            System.loadLibrary("syncbase");
            sOpts = opts;
            io.v.syncbase.internal.Service.Init(sOpts.rootDir, sOpts.testLogin);
            if (isLoggedIn()) {
                io.v.syncbase.internal.Service.Serve();
            }

        } catch (VError e) {
            chainThrow("initializing Syncbase", e);
        }
    }

    /**
     * Returns a Database object. Return null if the user is not currently logged in.
     */
    public static Database database() throws SyncbaseException {
        try {
            if (!isLoggedIn()) {
                return null;
            }
            if (sDatabase != null) {
                // TODO(sadovsky): Check that opts matches original opts (sOpts)?
                return sDatabase;
            }
            sDatabase = new Database(Service.database(DB_NAME));
            return sDatabase;

        } catch (VError e) {
            chainThrow("getting the database", e);
            throw new AssertionError("never happens");
        }
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
        try {
            return new User(getAliasFromBlessingPattern(getPersonalBlessingString()));
        } catch (SyncbaseException e) {
            return null;
        }
    }

    public interface LoginCallback {
        void onSuccess();
        void onError(Throwable e);
    }

    /**
     * Logs in the user on Android.
     * The user selects an account through an account picker flow and is logged into Syncbase.
     * Note: This default account flow is currently restricted to Google accounts.
     *
     * @param activity The Android activity where login will occur.
     * @param cb       The callback to call when the login was done.
     */
    public static void loginAndroid(Activity activity, final LoginCallback cb) {
        FragmentTransaction transaction = activity.getFragmentManager().beginTransaction();
        LoginFragment fragment = new LoginFragment();
        fragment.setTokenReceiver(new LoginFragment.TokenReceiver() {
            @Override
            public void receiveToken(String token) {
                Syncbase.login(token, User.PROVIDER_GOOGLE, cb);
            }
        });
        transaction.add(fragment, UUID.randomUUID().toString());
        transaction.commit();  // This will invoke the fragment's onCreate() immediately.
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

        new Thread(new Runnable() {
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
                    sUserdataCollection = sDatabase.createNamedCollection(
                            USERDATA_NAME,
                            new DatabaseHandle.CollectionOptions().setWithoutSyncgroup(true));
                    if (!sOpts.disableUserdataSyncgroup) {
                        Syncgroup syncgroup = sUserdataCollection.getSyncgroup();
                        // Join-Or-Create pattern. If join fails, create the syncgroup instead.
                        // Note: Syncgroup merge does not exist yet, so this may potentially lead
                        // to split-brain syncgroups. This is exacerbated by lack of cloud instance.
                        try {
                            syncgroup.join();
                        } catch(VError e) {
                            syncgroup.createIfMissing(ImmutableList.of(sUserdataCollection));
                        }
                        Database.AddWatchChangeHandlerOptions opts = new Database
                                .AddWatchChangeHandlerOptions.Builder().
                                setShowUserdataCollectionRow(true).build();
                        sDatabase.addWatchChangeHandler(new UserdataWatchHandler(), opts);
                    }
                    sOpts.callbackExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            cb.onSuccess();
                        }
                    });
                } catch (Throwable e) {
                    cb.onError(e);
                }
            }
        }).start();
    }

    private static class UserdataWatchHandler implements Database.WatchChangeHandler {
        @Override
        public void onInitialState(Iterator<WatchChange> values) {
            onWatchChange(values);
        }

        @Override
        public void onChangeBatch(Iterator<WatchChange> changes) {
            onWatchChange(changes);
        }

        @Override
        public void onError(Throwable e) {
            throw new RuntimeException(e);
        }

        private void onWatchChange(Iterator<WatchChange> changes) {
            while (changes.hasNext()) {
                WatchChange watchChange = changes.next();
                if (watchChange.getCollectionId().getName().equals(USERDATA_NAME) &&
                        watchChange.getEntityType() == WatchChange.EntityType.ROW &&
                        watchChange.getChangeType() == WatchChange.ChangeType.PUT &&
                        watchChange.getRowKey().startsWith(USERDATA_COLLECTION_PREFIX)) {
                    try {
                        String encodedId = watchChange.getRowKey().
                                substring(Syncbase.USERDATA_COLLECTION_PREFIX.length());
                        sDatabase.getSyncgroup(Id.decode(encodedId)).join();
                    } catch (VError vError) {
                        vError.printStackTrace();
                        System.err.println(vError.toString());
                    }
                }
            }
        }
    }

    static void addToUserdata(Id id) throws SyncbaseException {
        sUserdataCollection.put(Syncbase.USERDATA_COLLECTION_PREFIX + id.encode(), true);
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
                    public void onPeer(final NeighborhoodPeer peer) {
                        final SettableFuture<Boolean> setFuture = SettableFuture.create();
                        Syncbase.sOpts.callbackExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                User u = new User(getAliasFromBlessingPattern(peer.blessings));
                                if (peer.isLost) {
                                    cb.onLost(u);
                                } else {
                                    cb.onFound(u);
                                }
                                setFuture.set(true);
                            }
                        });
                        try {
                            setFuture.get();
                        } catch (InterruptedException | ExecutionException e) {
                            e.printStackTrace();
                            System.err.println(e.toString());
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

    public interface ScanNeighborhoodForUsersCallback {
        void onFound(User user);
        void onLost(User user);
        void onError(Throwable e);
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

    static String getPersonalBlessingString() throws SyncbaseException {
        try {

            return Blessings.UserBlessingFromContext();

        } catch(VError e) {
            chainThrow("getting certificates from context", e);
            throw new AssertionError("never happens");
        }
    }

    static Permissions defaultDatabasePerms() throws SyncbaseException {
        // TODO(sadovsky): Revisit these default perms, which were copied from the Todos app.
        Map anyone = ImmutableMap.of(Permissions.IN, ImmutableList.of("..."));
        Map selfAndCloud = selfAndCloud();
        return new Permissions(ImmutableMap.of(
                Permissions.Tags.RESOLVE, anyone,
                Permissions.Tags.READ, selfAndCloud,
                Permissions.Tags.WRITE, selfAndCloud,
                Permissions.Tags.ADMIN, selfAndCloud));
    }

    static Permissions defaultCollectionPerms() throws SyncbaseException {
        // TODO(sadovsky): Revisit these default perms, which were copied from the Todos app.
        Map selfAndCloud = selfAndCloud();
        return new Permissions(ImmutableMap.of(
                Permissions.Tags.READ, selfAndCloud,
                Permissions.Tags.WRITE, selfAndCloud,
                Permissions.Tags.ADMIN, selfAndCloud));
    }

    static Permissions defaultSyncgroupPerms() throws SyncbaseException {
        // TODO(sadovsky): Revisit these default perms, which were copied from the Todos app.
        Map selfAndCloud = selfAndCloud();
        return new Permissions(ImmutableMap.of(
                Permissions.Tags.READ, selfAndCloud,
                Permissions.Tags.ADMIN, selfAndCloud));
    }
}