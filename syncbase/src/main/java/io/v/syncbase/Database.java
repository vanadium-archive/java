// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.v.v23.VFutures;
import io.v.v23.syncbase.DatabaseCore;
import io.v.v23.verror.ExistException;
import io.v.v23.verror.VException;

public class Database implements DatabaseHandle {
    private final io.v.v23.syncbase.Database mVDatabase;

    protected Database(io.v.v23.syncbase.Database vDatabase) {
        try {
            VFutures.sync(vDatabase.create(Syncbase.getVContext(), Syncbase.defaultPerms()));
        } catch (ExistException e) {
            // Database already exists, presumably from a previous run of the app.
        } catch (VException e) {
            throw new RuntimeException("Failed to create database", e);
        }
        mVDatabase = vDatabase;
    }

    public Id getId() {
        return new Id(mVDatabase.id());
    }

    public Collection collection(String name, CollectionOptions opts) {
        // TODO(sadovsky): If !opts.withoutSyncgroup, create syncgroup and update userdata syncgroup.
        return new Collection(this, mVDatabase.getCollection(new io.v.v23.services.syncbase.Id(Syncbase.getPersonalBlessingString(), name)), true);
    }

    protected static Collection getCollectionImpl(Database database, DatabaseCore vDbCore, Id id) {
        // TODO(sadovsky): Consider throwing an exception or returning null if the collection does
        // not exist.
        return new Collection(database, vDbCore.getCollection(id.toVId()), false);
    }

    public Collection getCollection(Id id) {
        return getCollectionImpl(this, mVDatabase, id);
    }

    // Exposed as a static function so that the implementation can be shared between Database and
    // BatchDatabase.
    protected static Iterator<Collection> getCollectionsImpl(Database database, DatabaseCore vDbCore) {
        List<io.v.v23.services.syncbase.Id> vIds;
        try {
            vIds = VFutures.sync(vDbCore.listCollections(Syncbase.getVContext()));
        } catch (VException e) {
            throw new RuntimeException("listCollections failed", e);
        }
        ArrayList<Collection> cxs = new ArrayList<>(vIds.size());
        for (io.v.v23.services.syncbase.Id vId : vIds) {
            cxs.add(new Collection(database, vDbCore.getCollection(vId), false));
        }
        return cxs.iterator();
    }

    public Iterator<Collection> getCollections() {
        return getCollectionsImpl(this, mVDatabase);
    }

    public static class SyncgroupOptions {
        // TODO(sadovsky): Fill this in.
    }

    // FOR ADVANCED USERS. Creates syncgroup and adds it to the user's "userdata" collection, as
    // needed. Idempotent.
    public Syncgroup syncgroup(String name, Collection[] collections, SyncgroupOptions opts) {
        throw new RuntimeException("Not implemented");
    }

    public Syncgroup getSyncgroup(Id id) {
        // TODO(sadovsky): Consider throwing an exception or returning null if the syncgroup does
        // not exist.
        return new Syncgroup(id, mVDatabase.getSyncgroup(id.toVId()));
    }

    public Iterator<Syncgroup> getSyncgroups() {
        throw new RuntimeException("Not implemented");
    }

    public static class AddSyncgroupInviteHandlerOptions {
        // TODO(sadovsky): Fill this in.
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

    public BatchDatabase beginBatch(BatchOptions opts) {
        io.v.v23.syncbase.BatchDatabase vBatchDatabase;
        try {
            vBatchDatabase = VFutures.sync(mVDatabase.beginBatch(Syncbase.getVContext(), opts.toVBatchOptions()));
        } catch (VException e) {
            throw new RuntimeException("beginBatch failed", e);
        }
        return new BatchDatabase(this, vBatchDatabase);
    }

    public static class AddWatchChangeHandlerOptions {
        public byte[] resumeMarker;
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
