// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import io.v.v23.InputChannel;
import io.v.v23.InputChannelCallback;
import io.v.v23.InputChannels;
import io.v.v23.VFutures;
import io.v.v23.services.syncbase.CollectionRowPattern;
import io.v.v23.verror.ExistException;
import io.v.v23.verror.VException;

/**
 * A set of collections and syncgroups.
 */
public class Database extends DatabaseHandle {
    private final io.v.v23.syncbase.Database mVDatabase;

    private interface CancelFunc {
        void run();
    }

    private Map<SyncgroupInviteHandler, CancelFunc> mSyncgroupInviteHandlers;
    private Map<WatchChangeHandler, CancelFunc> mWatchChangeHandlers;

    protected void createIfMissing() {
        try {
            VFutures.sync(mVDatabase.create(Syncbase.getVContext(), Syncbase.defaultPerms()));
        } catch (ExistException e) {
            // Database already exists, presumably from a previous run of the app.
        } catch (VException e) {
            throw new RuntimeException("Failed to create database", e);
        }
    }

    protected Database(io.v.v23.syncbase.Database vDatabase) {
        super(vDatabase);
        mVDatabase = vDatabase;
        mSyncgroupInviteHandlers = new HashMap<>();
        mWatchChangeHandlers = new HashMap<>();
    }

    @Override
    public Collection collection(String name, CollectionOptions opts) {
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
     * @return the syncgroup
     */
    public Syncgroup syncgroup(String name, List<Collection> collections, SyncgroupOptions opts) {
        if (collections.isEmpty()) {
            throw new RuntimeException("No collections specified");
        }
        Id id = new Id(collections.get(0).getId().getBlessing(), name);
        for (Collection cx : collections) {
            if (!cx.getId().getBlessing().equals(id.getBlessing())) {
                throw new RuntimeException("Collections must all have the same creator");
            }
        }
        Syncgroup res = new Syncgroup(mVDatabase.getSyncgroup(id.toVId()), this, id);
        res.createIfMissing(collections);
        return res;
    }

    /**
     * Calls {@code syncgroup(name, collections, opts)} with default {@code SyncgroupOptions}.
     */
    public Syncgroup syncgroup(String name, List<Collection> collections) {
        return syncgroup(name, collections, new SyncgroupOptions());
    }

    /**
     * Returns the syncgroup with the given id.
     */
    public Syncgroup getSyncgroup(Id id) {
        // TODO(sadovsky): Consider throwing an exception or returning null if the syncgroup does
        // not exist.
        return new Syncgroup(mVDatabase.getSyncgroup(id.toVId()), this, id);
    }

    /**
     * Returns an iterator over all syncgroups in the database.
     */
    public Iterator<Syncgroup> getSyncgroups() {
        List<io.v.v23.services.syncbase.Id> vIds;
        try {
            vIds = VFutures.sync(mVDatabase.listSyncgroups(Syncbase.getVContext()));
        } catch (VException e) {
            throw new RuntimeException("listSyncgroups failed", e);
        }
        ArrayList<Syncgroup> sgs = new ArrayList<>(vIds.size());
        for (io.v.v23.services.syncbase.Id vId : vIds) {
            sgs.add(new Syncgroup(mVDatabase.getSyncgroup(vId), this, new Id(vId)));
        }
        return sgs.iterator();
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
    public abstract class SyncgroupInviteHandler {
        /**
         * Called when a syncgroup invitation is discovered. Clients typically handle invites by
         * calling {@code acceptSyncgroupInvite} or {@code ignoreSyncgroupInvite}.
         */
        void onInvite(SyncgroupInvite invite) {
        }

        /**
         * Called when an error occurs while scanning for syncgroup invitations. Once
         * {@code onError} is called, no other methods will be called on this handler.
         */
        void onError(Throwable e) {
        }
    }

    /**
     * Notifies {@code h} of any existing syncgroup invites, and of all subsequent new invites.
     */
    public void addSyncgroupInviteHandler(SyncgroupInviteHandler h, AddSyncgroupInviteHandlerOptions opts) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Makes it so {@code h} stops receiving notifications.
     */
    public void removeSyncgroupInviteHandler(SyncgroupInviteHandler h) {
        CancelFunc cancel = mSyncgroupInviteHandlers.remove(h);
        if (cancel != null) {
            cancel.run();
        }
    }

    /**
     * Makes it so all syncgroup invite handlers stop receiving notifications.
     */
    public void removeAllSyncgroupInviteHandlers() {
        for (CancelFunc cancel : mSyncgroupInviteHandlers.values()) {
            cancel.run();
        }
        mSyncgroupInviteHandlers.clear();
    }

    /**
     * Joins the syncgroup associated with the given invite and adds it to the user's "userdata"
     * collection, as needed.
     *
     * @param invite the invite
     * @return a handle for the joined syncgroup
     */
    public Syncgroup acceptSyncgroupInvite(SyncgroupInvite invite) {
        // TODO(sadovsky): Should we add "accept" and "ignore" methods to the SyncgroupInvite class,
        // or should we treat it as a POJO (with no reference to Database)?
        // TODO(sadovsky): Make this method async.
        throw new RuntimeException("Not implemented");
    }

    /**
     * Records that the user has ignored this invite, such that it's never surfaced again.
     *
     * @param invite the invite
     */
    public void ignoreSyncgroupInvite(SyncgroupInvite invite) {
        // Note: This will be one of the last things we implement.
        // TODO(sadovsky): Maybe document how to read/write rejection metadata in the userdata
        // collection, for advanced users.
        throw new RuntimeException("Not implemented");
    }

    /**
     * Options for a batch.
     */
    public static class BatchOptions {
        public boolean readOnly;

        protected io.v.v23.services.syncbase.BatchOptions toVBatchOptions() {
            io.v.v23.services.syncbase.BatchOptions res = new io.v.v23.services.syncbase.BatchOptions();
            res.setReadOnly(true);
            return res;
        }
    }

    /**
     * Designed for use in {@code runInBatch}.
     */
    public interface BatchOperation {
        void run(BatchDatabase db);
    }

    /**
     * Runs the given operation in a batch, managing retries and commit/abort. Writable batches are
     * committed, retrying if commit fails due to a concurrent batch. Read-only batches are aborted.
     *
     * @param op   the operation to run
     * @param opts options for this batch
     */
    public void runInBatch(BatchOperation op, BatchOptions opts) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Creates a new batch. Instead of calling this function directly, clients are encouraged to use
     * the {@code runInBatch} helper function, which detects "concurrent batch" errors and handles
     * retries internally.
     * <p>
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
     * <p>
     * Once a batch has been committed or aborted, subsequent method calls will fail with no
     * effect.
     * <p>
     * Concurrency semantics can be configured using BatchOptions.
     *
     * @param opts options for this batch
     * @return the batch handle
     */
    public BatchDatabase beginBatch(BatchOptions opts) {
        io.v.v23.syncbase.BatchDatabase vBatchDatabase;
        try {
            vBatchDatabase = VFutures.sync(mVDatabase.beginBatch(Syncbase.getVContext(), opts.toVBatchOptions()));
        } catch (VException e) {
            throw new RuntimeException("beginBatch failed", e);
        }
        return new BatchDatabase(vBatchDatabase);
    }

    /**
     * Options for {@code addWatchChangeHandler}.
     */
    public static class AddWatchChangeHandlerOptions {
        public byte[] resumeMarker;
    }

    /**
     * Handles observed changes to the database.
     */
    public abstract class WatchChangeHandler {
        // TODO(sadovsky): Consider adopting Aaron's suggestion of combining onInitialState and
        // onChangeBatch into a single method, to make things simpler for developers who don't want
        // to apply deltas to their in-memory data structures:
        // void onChangeBatch(Iterator<WatchChange> values, Iterator<WatchChange> changes)

        /**
         * Called once, when a watch change handler is added, to provide the initial state of the
         * values being watched.
         */
        void onInitialState(Iterator<WatchChange> values) {
        }

        /**
         * Called whenever a batch of changes is committed to the database. Individual puts/deletes
         * surface as a single-change batch.
         */
        void onChangeBatch(Iterator<WatchChange> changes) {
        }

        /**
         * Called when an error occurs while watching for changes. Once {@code onError} is called,
         * no other methods will be called on this handler.
         */
        void onError(Throwable e) {
        }
    }

    /**
     * Notifies {@code h} of initial state, and of all subsequent changes to this database.
     */
    public void addWatchChangeHandler(final WatchChangeHandler h, AddWatchChangeHandlerOptions opts) {
        // Note: Eventually we'll add a watch variant that takes a query, where the query can be
        // constructed using some sort of query builder API.
        // TODO(sadovsky): Support specifying resumeMarker. Note, watch-from-resumeMarker may be
        // problematic in that we don't track the governing ACL for changes in the watch log.
        if (opts.resumeMarker.length != 0) {
            throw new RuntimeException("Specifying resumeMarker is not yet supported");
        }
        InputChannel<io.v.v23.syncbase.WatchChange> ic = mVDatabase.watch(Syncbase.getVContext(), ImmutableList.of(new CollectionRowPattern("%", "%", "%")));
        ListenableFuture<Void> future = InputChannels.withCallback(ic, new InputChannelCallback<io.v.v23.syncbase.WatchChange>() {
            private boolean mGotFirstBatch = false;
            private List<WatchChange> mBatch = new ArrayList<>();

            @Override
            public ListenableFuture<Void> onNext(io.v.v23.syncbase.WatchChange vChange) {
                WatchChange change = new WatchChange(vChange);
                // Ignore changes to userdata collection.
                if (change.getCollectionId().getName().equals(Syncbase.USERDATA_SYNCGROUP_NAME)) {
                    return null;
                }
                mBatch.add(change);
                if (!change.isContinued()) {
                    if (!mGotFirstBatch) {
                        mGotFirstBatch = true;
                        h.onInitialState(mBatch.iterator());
                    } else {
                        h.onChangeBatch(mBatch.iterator());
                    }
                    mBatch.clear();
                }
                return null;
            }
        });
        Futures.addCallback(future, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
            }

            @Override
            public void onFailure(Throwable e) {
                h.onError(e);
            }
        });
    }

    /**
     * Makes it so {@code h} stops receiving notifications.
     */
    public void removeWatchChangeHandler(WatchChangeHandler h) {
        CancelFunc cancel = mWatchChangeHandlers.remove(h);
        if (cancel != null) {
            cancel.run();
        }
    }

    /**
     * Makes it so all watch change handlers stop receiving notifications.
     */
    public void removeAllWatchChangeHandlers() {
        for (CancelFunc cancel : mWatchChangeHandlers.values()) {
            cancel.run();
        }
        mWatchChangeHandlers.clear();
    }
}
