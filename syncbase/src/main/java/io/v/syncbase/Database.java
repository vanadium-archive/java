// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.v.v23.VFutures;
import io.v.v23.verror.ExistException;
import io.v.v23.verror.VException;

public class Database extends DatabaseHandle {
    private final io.v.v23.syncbase.Database mVDatabase;

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
    }

    public Collection collection(String name, CollectionOptions opts) {
        // TODO(sadovsky): If !opts.withoutSyncgroup, create syncgroup and update userdata
        // syncgroup.
        Collection res = getCollection(new Id(Syncbase.getPersonalBlessingString(), name));
        res.createIfMissing();
        return res;
    }

    public static class SyncgroupOptions {
        // TODO(sadovsky): Fill this in.
    }

    // FOR ADVANCED USERS. Creates syncgroup and adds it to the user's "userdata" collection, as
    // needed. Idempotent.
    public Syncgroup syncgroup(String name, List<Collection> collections, SyncgroupOptions opts) {
        Id id = new Id(collections.get(0).getId().getBlessing(), name);
        Syncgroup res = new Syncgroup(mVDatabase.getSyncgroup(id.toVId()), this, id);
        res.createIfMissing(collections);
        return res;
    }

    public Syncgroup syncgroup(String name, List<Collection> collections) {
        return syncgroup(name, collections, new SyncgroupOptions());
    }


    public Syncgroup getSyncgroup(Id id) {
        // TODO(sadovsky): Consider throwing an exception or returning null if the syncgroup does
        // not exist.
        return new Syncgroup(mVDatabase.getSyncgroup(id.toVId()), this, id);
    }

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

    public static class AddSyncgroupInviteHandlerOptions {
        // TODO(sadovsky): Fill this in.
    }

    public abstract class SyncgroupInviteHandler {
        void onInvite(SyncgroupInvite invite) {
        }

        void onError(Exception e) {
        }
    }

    // Notifies 'h' of any existing syncgroup invites, and of all subsequent new invites.
    public void addSyncgroupInviteHandler(SyncgroupInviteHandler h, AddSyncgroupInviteHandlerOptions opts) {
        throw new RuntimeException("Not implemented");
    }

    public void removeSyncgroupInviteHandler(SyncgroupInviteHandler h) {
        throw new RuntimeException("Not implemented");
    }

    public void removeSyncgroupInviteHandlers() {
        throw new RuntimeException("Not implemented");
    }

    // Joins syncgroup and adds it to the user's "userdata" collection, as needed.
    // TODO(sadovsky): Should we add "accept" and "ignore" methods to the SyncgroupInvite class, or
    // should we treat it as a POJO (with no reference to Database)?
    // TODO(sadovsky): Make this method async.
    public Syncgroup acceptSyncgroupInvite(SyncgroupInvite invite) {
        throw new RuntimeException("Not implemented");
    }

    // Records that the user has ignored this invite, such that it's never surfaced again.
    // Note: This will be one of the last things we implement.
    public void ignoreSyncgroupInvite(SyncgroupInvite invite) {
        throw new RuntimeException("Not implemented");
    }

    public static class BatchOptions {
        // TODO(sadovsky): Fill this in further.

        protected io.v.v23.services.syncbase.BatchOptions toVBatchOptions() {
            return new io.v.v23.services.syncbase.BatchOptions();
        }
    }

    public interface BatchOperation {
        void run(BatchDatabase db);
    }

    public void runInBatch(BatchOperation op, BatchOptions opts) {
        throw new RuntimeException("Not implemented");
    }

    public BatchDatabase beginBatch(BatchOptions opts) {
        io.v.v23.syncbase.BatchDatabase vBatchDatabase;
        try {
            vBatchDatabase = VFutures.sync(mVDatabase.beginBatch(Syncbase.getVContext(), opts.toVBatchOptions()));
        } catch (VException e) {
            throw new RuntimeException("beginBatch failed", e);
        }
        return new BatchDatabase(vBatchDatabase);
    }

    public static class AddWatchChangeHandlerOptions {
        public byte[] resumeMarker;
    }

    public abstract class WatchChangeHandler {
        // TODO(sadovsky): Consider adopting Aaron's suggestion of combining onInitialState and
        // onChangeBatch into a single method, to make things simpler for developers who don't want to
        // apply deltas to their in-memory data structures:
        // void onChangeBatch(Iterator<WatchChange> values, Iterator<WatchChange> changes)

        void onInitialState(Iterator<WatchChange> values) {
        }

        void onChangeBatch(Iterator<WatchChange> changes) {
        }

        void onError(Exception e) {
        }
    }

    // Notifies 'h' of initial state, and of all subsequent changes to this database.
    // Note: Eventually we'll add a watch variant that takes a query, where the query can be
    // constructed using some sort of query builder API.
    public void addWatchChangeHandler(WatchChangeHandler h, AddWatchChangeHandlerOptions opts) {
        // TODO(sadovsky): Support specifying resumeMarker.
        if (opts.resumeMarker.length != 0) {
            throw new RuntimeException("Specifying resumeMarker is not yet supported");
        }
        throw new RuntimeException("Not implemented");
    }

    public void removeWatchChangeHandler(WatchChangeHandler h) {
        throw new RuntimeException("Not implemented");
    }

    public void removeAllWatchChangeHandlers() {
        throw new RuntimeException("Not implemented");
    }
}
