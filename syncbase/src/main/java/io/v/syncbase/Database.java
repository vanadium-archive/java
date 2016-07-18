// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.SettableFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import io.v.syncbase.core.CollectionRowPattern;
import io.v.syncbase.core.SyncgroupMemberInfo;
import io.v.syncbase.core.VError;
import io.v.syncbase.exception.SyncbaseException;

import static io.v.syncbase.exception.Exceptions.chainThrow;

/**
 * A set of collections and syncgroups.
 * To get a Database handle, call {@code Syncbase.database}.
 */
public class Database extends DatabaseHandle {
    private final io.v.syncbase.core.Database mCoreDatabase;

    private final Object mSyncgroupInviteHandlersMu = new Object();
    private final Object mWatchChangeHandlersMu = new Object();
    private final Map<SyncgroupInviteHandler, Long> mSyncgroupInviteHandlers = new HashMap<>();
    private final Map<WatchChangeHandler, Runnable> mWatchChangeHandlers = new HashMap<>();

    Database(io.v.syncbase.core.Database coreDatabase) {
        super(coreDatabase);
        mCoreDatabase = coreDatabase;
    }

    void createIfMissing() throws SyncbaseException {
        try {
            mCoreDatabase.create(Syncbase.defaultDatabasePerms());
        } catch (VError vError) {
            if (vError.id.equals(VError.EXIST)) {
                return;
            }
            chainThrow("creating database", vError);
        }
    }

    @Override
    public Collection createCollection(CollectionOptions opts) throws SyncbaseException {
        String name = opts.prefix + "_" + UUID.randomUUID().toString().replaceAll("-", "");
        return createNamedCollection(name, opts);
    }

    Collection createNamedCollection(String name, CollectionOptions opts) throws SyncbaseException {
        Collection res = getCollection(new Id(Syncbase.getPersonalBlessingString(), name));
        res.createIfMissing();
        // TODO(sadovsky): Unwind collection creation on syncgroup creation failure? It would be
        // nice if we could create the collection and syncgroup in a batch.
        if (!opts.withoutSyncgroup) {
            syncgroup(name, ImmutableList.of(res), new SyncgroupOptions());
        }
        return res;
    }


    /**
     * Returns a reference to the userdata collection. Returns null if the user is not currently
     * logged in.
     */
    public Collection getUserdataCollection() throws SyncbaseException {
        return getCollection(new Id(Syncbase.getPersonalBlessingString(), Syncbase.USERDATA_NAME));
    }

    /**
     * FOR ADVANCED USERS. Options for syncgroup creation.
     */
    public static class SyncgroupOptions {
        // TODO(sadovsky): Fill this in.
    }

    /**
     * FOR ADVANCED USERS. Creates syncgroup and adds it to the user's "userdata" collection, as
     * needed. Idempotent. The id of the new syncgroup will include the creator's user id and the
     * given syncgroup name. Requires that all collections were created by the current user.
     *
     * @param name        name of the syncgroup
     * @param collections collections in the syncgroup
     * @param opts        options for syncgroup creation
     * @throws IllegalArgumentException if no collections or collections don't all have same creator
     * @return the syncgroup
     */
    public Syncgroup syncgroup(String name, List<Collection> collections, SyncgroupOptions opts)
            throws SyncbaseException {
        if (collections.isEmpty()) {
            throw new IllegalArgumentException("No collections specified");
        }
        Id id = new Id(collections.get(0).getId().getBlessing(), name);
        for (Collection collection : collections) {
            if (!collection.getId().getBlessing().equals(id.getBlessing())) {
                throw new IllegalArgumentException("Collections must all have the same creator");
            }
        }
        Syncgroup syncgroup = new Syncgroup(mCoreDatabase.syncgroup(id.toCoreId()), this);
        syncgroup.createIfMissing(collections);
        // Remember this syncgroup in the userdata collection. The value doesn't matter, but since
        // VOM won't accept null, use a boolean.
        // Note: We may eventually want to use the value to deal with rejected invitations.
        Syncbase.addToUserdata(id);
        return syncgroup;
    }

    /**
     * Calls {@code syncgroup(name, collections, opts)} with default {@code SyncgroupOptions}.
     */
    public Syncgroup syncgroup(String name, List<Collection> collections) throws SyncbaseException {
        return syncgroup(name, collections, new SyncgroupOptions());
    }

    /**
     * Returns the syncgroup with the given id.
     */
    public Syncgroup getSyncgroup(Id id) {
        // TODO(sadovsky): Consider throwing an exception or returning null if the syncgroup does
        // not exist. But note, a syncgroup can get destroyed via sync after a client obtains a
        // handle for it, so perhaps we should instead add an 'exists' method.
        return new Syncgroup(mCoreDatabase.syncgroup(id.toCoreId()), this);
    }

    /**
     * Returns an iterator over all syncgroups in the database.
     */
    public Iterator<Syncgroup> getSyncgroups() throws SyncbaseException {
        try {

            ArrayList<Syncgroup> syncgroups = new ArrayList<>();
            for (io.v.syncbase.core.Id id : mCoreDatabase.listSyncgroups()) {
                syncgroups.add(getSyncgroup(new Id(id)));
            }
            return syncgroups.iterator();

        } catch (VError e) {
            chainThrow("getting syncgroups of database", mCoreDatabase.id().name, e);
            throw new AssertionError("never happens");
        }
    }

    /**
     * Options for {@code addSyncgroupInviteHandler}.
     */
    public static class AddSyncgroupInviteHandlerOptions {
        // TODO(sadovsky): Fill this in.
    }

    /**
     * Handles discovered syncgroup invites.
     */
    public interface SyncgroupInviteHandler {
        /**
         * Called when a syncgroup invitation is discovered. Clients typically handle invites by
         * calling {@code acceptSyncgroupInvite} or {@code ignoreSyncgroupInvite}.
         */
        void onInvite(SyncgroupInvite invite);

        /**
         * Called when an error occurs while scanning for syncgroup invitations. Once
         * {@code onError} is called, no other methods will be called on this handler.
         */
        void onError(Throwable e);
    }

    // TODO(sadovsky): Document which thread the handler methods are called on.

    /**
     * Notifies {@code h} of any existing syncgroup invites, and of all subsequent new invites.
     */
    public void addSyncgroupInviteHandler(final SyncgroupInviteHandler h, AddSyncgroupInviteHandlerOptions opts) {
        synchronized (mSyncgroupInviteHandlersMu) {
            try {
                long scanId = io.v.syncbase.internal.Database.SyncgroupInvitesNewScan(
                        getId().encode(),
                        new io.v.syncbase.internal.Database.SyncgroupInvitesCallbacks() {

                    @Override
                    public void onInvite(final io.v.syncbase.core.SyncgroupInvite invite) {
                        final SettableFuture<Boolean> setFuture = SettableFuture.create();
                        Syncbase.sOpts.mCallbackExecutor.execute(new Runnable() {
                            @Override
                            public void run() {
                                h.onInvite(new SyncgroupInvite(new Id(invite.syncgroup),
                                        invite.blessingNames));
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
                mSyncgroupInviteHandlers.put(h, scanId);
            } catch (VError vError) {
                h.onError(vError);
            }
        }
    }

    /**
     * Calls {@code addSyncgroupInviteHandler(h, opts)} with default
     * {@code AddSyncgroupInviteHandlerOptions}.
     */
    public void addSyncgroupInviteHandler(SyncgroupInviteHandler h) {
        addSyncgroupInviteHandler(h, new AddSyncgroupInviteHandlerOptions());
    }

    /**
     * Makes it so {@code h} stops receiving notifications.
     */
    public void removeSyncgroupInviteHandler(SyncgroupInviteHandler h) {
        synchronized (mSyncgroupInviteHandlersMu) {
            Long scanId = mSyncgroupInviteHandlers.remove(h);
            if (scanId != null) {
                io.v.syncbase.internal.Database.SyncgroupInvitesStopScan(scanId);
            }
        }
    }

    /**
     * Makes it so all syncgroup invite handlers stop receiving notifications.
     */
    public void removeAllSyncgroupInviteHandlers() {
        synchronized (mSyncgroupInviteHandlersMu) {
            for (Long scanId : mSyncgroupInviteHandlers.values()) {
                io.v.syncbase.internal.Database.SyncgroupInvitesStopScan(scanId);
            }
            mSyncgroupInviteHandlers.clear();
        }
    }

    public interface AcceptSyncgroupInviteCallback {
         void onSuccess(Syncgroup sg);
         void onFailure(Throwable e);
    }

    /**
     * Joins the syncgroup associated with the given invite and adds it to the user's "userdata"
     * collection, as needed. The passed callback is called on the current thread.
     *
     * @param invite the syncgroup invite
     * @param cb     the callback to call with the syncgroup handle
     */
    public void acceptSyncgroupInvite(final SyncgroupInvite invite,
                                      final AcceptSyncgroupInviteCallback cb) {
        // TODO(sadovsky): Should we add "accept" and "ignore" methods to the SyncgroupInvite class,
        // or should we treat it as a POJO (with no reference to Database)?
        final io.v.syncbase.core.Syncgroup coreSyncgroup =
                mCoreDatabase.syncgroup(invite.getId().toCoreId());
        final Database database = this;
        // TODO(razvanm): Figure out if we should use an AsyncTask or something else.
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String publishName = Syncbase.sOpts.mCloudName; // ok if null
                    List<String> expectedBlessings = invite.getInviterBlessingNames();
                    if (Syncbase.sOpts.mCloudAdmin != null) {
                        expectedBlessings.add(Syncbase.sOpts.mCloudAdmin);
                    }
                    coreSyncgroup.join(publishName, expectedBlessings, new SyncgroupMemberInfo());
                    Syncbase.addToUserdata(invite.getId());
                } catch (VError | SyncbaseException e) {
                    cb.onFailure(e);
                    return;
                }
                cb.onSuccess(new Syncgroup(coreSyncgroup, database));
            }
        }).start();
    }

    /**
     * Records that the user has ignored this invite, such that it's never surfaced again.
     *
     * @param invite the syncgroup invite
     */
    public void ignoreSyncgroupInvite(SyncgroupInvite invite) {
        // Note: This will be one of the last things we implement.
        // TODO(sadovsky): Maybe document how to read/write rejection metadata in the userdata
        // collection, for advanced users.
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Options for a batch.
     */
    public static class BatchOptions {
        public boolean readOnly;

        public io.v.syncbase.core.BatchOptions toCore() {
            io.v.syncbase.core.BatchOptions coreBatchOptions =
                    new io.v.syncbase.core.BatchOptions();
            coreBatchOptions.readOnly = readOnly;
            return coreBatchOptions;
        }
    }

    /**
     * Designed for use in {@code runInBatch}.
     */
    public interface BatchOperation {
        void run(BatchDatabase db) throws SyncbaseException;
    }

    /**
     * Runs the given operation in a batch, managing retries and commit/abort. Writable batches are
     * committed, retrying if commit fails due to a concurrent batch. Read-only batches are aborted.
     *
     * @param op   the operation to run
     * @param opts options for this batch
     */
    public void runInBatch(final BatchOperation op, BatchOptions opts) throws SyncbaseException {
        try {

            mCoreDatabase.runInBatch(new io.v.syncbase.core.Database.BatchOperation() {
                @Override
                public void run(io.v.syncbase.core.BatchDatabase batchDatabase) {
                    try {
                        op.run(new BatchDatabase(batchDatabase));
                    } catch (SyncbaseException e) {
                        e.printStackTrace();
                    }
                }
            }, opts.toCore());

        } catch (VError e) {
            chainThrow("running batch operation in database", mCoreDatabase.id().name, e);
        }
    }

    /**
     * Runs the given operation in a batch, managing retries and commit/abort. Writable batches are
     * committed, retrying if commit fails due to a concurrent batch. Read-only batches are aborted.
     * Uses the default BatchOptions.
     *
     * @param op   the operation to run
     */
    public void runInBatch(final BatchOperation op) throws SyncbaseException {
        runInBatch(op, new BatchOptions());
    }

    /**
     * Creates a new batch. Instead of calling this function directly, clients are encouraged to use
     * the {@code runInBatch} helper function, which detects "concurrent batch" errors and handles
     * retries internally.
     * <p/>
     * Default concurrency semantics:
     * <ul>
     * <li>Reads (e.g. gets, scans) inside a batch operate over a consistent snapshot taken during
     * {@code beginBatch}, and will see the effects of prior writes performed inside the batch.</li>
     * <li>{@code commit} may fail with {@code ConcurrentBatchException}, indicating that after
     * {@code beginBatch} but before {@code commit}, some concurrent routine wrote to a key that
     * matches a key or row-range read inside this batch.</li>
     * <li>Other methods will never fail with error {@code ConcurrentBatchException}, even if it is
     * known that {@code commit} will fail with this error.</li>
     * </ul>
     * <p/>
     * Once a batch has been committed or aborted, subsequent method calls will fail with no
     * effect.
     * <p/>
     * Concurrency semantics can be configured using BatchOptions.
     *
     * @param opts options for this batch
     * @return the batch handle
     */
    public BatchDatabase beginBatch(BatchOptions opts) throws SyncbaseException {
        try {

            return new BatchDatabase(mCoreDatabase.beginBatch(opts.toCore()));

        } catch (VError e) {
            chainThrow("creating batch in database", mCoreDatabase.id().name, e);
            throw new AssertionError("never happens");
        }
    }

    /**
     * Options for {@code addWatchChangeHandler}.
     */
    public static class AddWatchChangeHandlerOptions {
        final byte[] resumeMarker;
        final String name;
        final String blessing;
        final String row;
        final boolean showUserdataCollectionRow;

        AddWatchChangeHandlerOptions(Builder builder) {
            resumeMarker = builder.resumeMarker;
            name = builder.name;
            blessing = builder.blessing;
            row = builder.row;
            showUserdataCollectionRow = builder.showUserdataCollectionRow;
        }

        CollectionRowPattern getCollectionRowPattern() {
            return new CollectionRowPattern(blessing, name, row);
        }

        public static class Builder {
            private byte[] resumeMarker;
            private String name = WILDCARD;
            private String blessing = WILDCARD;
            private String row = WILDCARD;
            private boolean showUserdataCollectionRow;

            private static final String WILDCARD = "%";

            public Builder setResumeMarker(byte[] resumeMarker) {
                this.resumeMarker = resumeMarker;
                return this;
            }

            public Builder setCollectionNamePrefix(String prefix) {
                // TODO(alexfandrianto): Unsafe. The prefix was not escaped. Incorrect if it has a
                // trailing backslash.
                name = prefix + WILDCARD;
                return this;
            }

            public Builder setCollectionId(Id id) {
                name = id.getName();
                blessing = id.getBlessing();
                return this;
            }

            public Builder setRowKeyPrefix(String prefix) {
                // TODO(alexfandrianto): Unsafe. The prefix was not escaped. Incorrect if it has a
                // trailing backslash.
                row = prefix + WILDCARD;
                return this;
            }

            public Builder setRowKey(String rowKey) {
                row = rowKey;
                return this;
            }

            Builder setShowUserdataCollectionRow(boolean shouldShow) {
                showUserdataCollectionRow = shouldShow;
                return this;
            }

            public AddWatchChangeHandlerOptions build() {
                return new AddWatchChangeHandlerOptions(this);
            }
        }
    }

    /**
     * Handles observed changes to the database.
     */
    public interface WatchChangeHandler {
        // TODO(sadovsky): Consider adopting Aaron's suggestion of combining onInitialState and
        // onChangeBatch into a single method, to make things simpler for developers who don't want
        // to apply deltas to their in-memory data structures:
        // void onChangeBatch(Iterator<WatchChange> values, Iterator<WatchChange> changes)

        /**
         * Called once, when a watch change handler is added, to provide the initial state of the
         * values being watched.
         */
        void onInitialState(Iterator<WatchChange> values);

        /**
         * Called whenever a batch of changes is committed to the database. Individual puts/deletes
         * surface as a single-change batch.
         */
        void onChangeBatch(Iterator<WatchChange> changes);

        /**
         * Called when an error occurs while watching for changes. Once {@code onError} is called,
         * no other methods will be called on this handler.
         */
        void onError(Throwable e);
    }

    // TODO(sadovsky): Document which thread the handler methods are called on.

    /**
     * Notifies {@code h} of initial state, and of all subsequent changes to this database.
     */
    public void addWatchChangeHandler(final WatchChangeHandler h, final AddWatchChangeHandlerOptions opts) {
        // Note: Eventually we'll add a watch variant that takes a query, where the query can be
        // constructed using some sort of query builder API.
        // TODO(sadovsky): Support specifying resumeMarker. Note, watch-from-resumeMarker may be
        // problematic in that we don't track the governing ACL for changes in the watch log.
        if (opts.resumeMarker != null && opts.resumeMarker.length != 0) {
            throw new UnsupportedOperationException("Specifying resumeMarker is not yet supported");
        }

        mCoreDatabase.watch(null, ImmutableList.of(opts.getCollectionRowPattern()),
                new io.v.syncbase.core.Database.WatchPatternsCallbacks() {
                    private boolean mGotFirstBatch = false;
                    private final List<WatchChange> mBatch = new ArrayList<>();

                    @Override
                    public void onChange(io.v.syncbase.core.WatchChange coreWatchChange) {
                        boolean isRoot = coreWatchChange.entityType ==
                                io.v.syncbase.core.WatchChange.EntityType.ROOT;
                        boolean isUserdataCollectionRow =
                                coreWatchChange.entityType ==
                                        io.v.syncbase.core.WatchChange.EntityType.ROW &&
                                coreWatchChange.collection.name.equals(Syncbase.USERDATA_NAME) &&
                                coreWatchChange.row.startsWith(Syncbase.USERDATA_COLLECTION_PREFIX);
                        if (!isRoot && (opts.showUserdataCollectionRow || !isUserdataCollectionRow)) {
                            mBatch.add(new WatchChange(coreWatchChange));
                        }
                        if (!coreWatchChange.continued) {
                            final SettableFuture<Boolean> setFuture = SettableFuture.create();
                            if (!mGotFirstBatch) {
                                mGotFirstBatch = true;
                                final List<WatchChange> cpBatch = mBatch;
                                Syncbase.sOpts.mCallbackExecutor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        h.onInitialState(cpBatch.iterator());
                                        setFuture.set(true);
                                    }
                                });
                            } else {
                                final List<WatchChange> cpBatch = mBatch;
                                Syncbase.sOpts.mCallbackExecutor.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        h.onChangeBatch(cpBatch.iterator());
                                        setFuture.set(true);
                                    }
                                });
                            }
                            try {
                                setFuture.get();
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                                System.err.println(e.toString());
                            }
                            mBatch.clear();
                        }
                    }

                    @Override
                    public void onError(VError vError) {
                        // TODO(sadovsky): Make sure cancellations are surfaced as such (or ignored).
                        h.onError(vError);
                    }
                });

        synchronized (mWatchChangeHandlersMu) {
            mWatchChangeHandlers.put(h, new Runnable() {
                @Override
                public void run() {
                    throw new UnsupportedOperationException("Not implemented");
                }
            });
        }
    }

    /**
     * Calls {@code addWatchChangeHandler(h, opts)} with default
     * {@code AddWatchChangeHandlerOptions}.
     */
    public void addWatchChangeHandler(WatchChangeHandler h) {
        addWatchChangeHandler(h, new AddWatchChangeHandlerOptions.Builder().build());
    }

    /**
     * Makes it so {@code h} stops receiving notifications.
     */
    public void removeWatchChangeHandler(WatchChangeHandler h) {
        synchronized (mWatchChangeHandlersMu) {
            Runnable cancel = mWatchChangeHandlers.remove(h);
            if (cancel != null) {
                cancel.run();
            }
        }
    }

    /**
     * Makes it so all watch change handlers stop receiving notifications.
     */
    public void removeAllWatchChangeHandlers() {
        synchronized (mWatchChangeHandlersMu) {
            for (Runnable cancel : mWatchChangeHandlers.values()) {
                cancel.run();
            }
            mWatchChangeHandlers.clear();
        }
    }
}
