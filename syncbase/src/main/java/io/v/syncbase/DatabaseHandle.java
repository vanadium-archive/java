// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.v.v23.VFutures;
import io.v.v23.syncbase.DatabaseCore;
import io.v.v23.verror.VException;

/**
 * Represents a handle to a database, possibly in a batch.
 */
public abstract class DatabaseHandle {
    protected DatabaseCore mVDatabaseCore;

    protected DatabaseHandle(DatabaseCore vDatabaseCore) {
        mVDatabaseCore = vDatabaseCore;
    }

    /**
     * Returns the id of this database.
     */
    Id getId() {
        return new Id(mVDatabaseCore.id());
    }

    /**
     * Options for collection creation.
     */
    public static class CollectionOptions {
        public boolean withoutSyncgroup;
    }

    /**
     * Creates a collection and an associated syncgroup, as needed. Idempotent. The id of the new
     * collection will include the creator's user id and the given collection name. Upon creation,
     * both the collection and syncgroup are {@code READ_WRITE} for the creator. Setting
     * {@code opts.withoutSyncgroup} prevents syncgroup creation. May only be called within a batch
     * if {@code opts.withoutSyncgroup} is set.
     *
     * @param name name of the collection
     * @param opts options for collection creation
     * @return the collection handle
     */
    abstract Collection collection(String name, CollectionOptions opts);

    /**
     * Calls {@code collection(name, opts)} with default {@code CollectionOptions}.
     */
    Collection collection(String name) {
        return collection(name, new CollectionOptions());
    }

    /**
     * Returns the collection with the given id.
     */
    Collection getCollection(Id id) {
        // TODO(sadovsky): Consider throwing an exception or returning null if the collection does
        // not exist.
        return new Collection(mVDatabaseCore.getCollection(id.toVId()), this);
    }

    /**
     * Returns an iterator over all collections in the database.
     */
    Iterator<Collection> getCollections() {
        List<io.v.v23.services.syncbase.Id> vIds;
        try {
            vIds = VFutures.sync(mVDatabaseCore.listCollections(Syncbase.getVContext()));
        } catch (VException e) {
            throw new RuntimeException("listCollections failed", e);
        }
        ArrayList<Collection> cxs = new ArrayList<>(vIds.size());
        for (io.v.v23.services.syncbase.Id vId : vIds) {
            cxs.add(new Collection(mVDatabaseCore.getCollection(vId), this));
        }
        return cxs.iterator();
    }
}
