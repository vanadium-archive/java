// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.util.ArrayList;
import java.util.Iterator;

import io.v.syncbase.core.VError;


/**
 * Represents a handle to a database, possibly in a batch.
 */
public abstract class DatabaseHandle {
    protected io.v.syncbase.core.DatabaseHandle mCoreDatabaseHandle;

    protected DatabaseHandle(io.v.syncbase.core.DatabaseHandle coreDatabaseHandle) {
        mCoreDatabaseHandle = coreDatabaseHandle;
    }

    /**
     * Returns the id of this database.
     */
    public Id getId() {
        return new Id(mCoreDatabaseHandle.id());
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
    public abstract Collection collection(String name, CollectionOptions opts) throws VError;

    /**
     * Calls {@code collection(name, opts)} with default {@code CollectionOptions}.
     */
    public Collection collection(String name) throws VError {
        return collection(name, new CollectionOptions());
    }

    /**
     * Returns the collection with the given id.
     */
    public Collection getCollection(Id id) {
        // TODO(sadovsky): Consider throwing an exception or returning null if the collection does
        // not exist. But note, a collection can get destroyed via sync after a client obtains a
        // handle for it, so perhaps we should instead add an 'exists' method.
        return new Collection(mCoreDatabaseHandle.collection(id.toCoreId()), this);
    }

    /**
     * Returns an iterator over all collections in the database.
     */
    public Iterator<Collection> getCollections() throws VError {
        ArrayList<Collection> collections = new ArrayList<>();
        for (io.v.syncbase.core.Id id : mCoreDatabaseHandle.listCollections()) {
            collections.add(getCollection(new Id(id)));
        }
        return collections.iterator();
    }
}
