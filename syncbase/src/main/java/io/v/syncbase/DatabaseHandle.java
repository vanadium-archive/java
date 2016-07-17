// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.util.ArrayList;
import java.util.Iterator;

import io.v.syncbase.core.VError;
import io.v.syncbase.exception.SyncbaseException;

import static io.v.syncbase.exception.Exceptions.chainThrow;


/**
 * Represents a handle to a database, possibly in a batch.
 */
public abstract class DatabaseHandle {
    private final io.v.syncbase.core.DatabaseHandle mCoreDatabaseHandle;

    protected static final String DEFAULT_COLLECTION_PREFIX = "cx";

    DatabaseHandle(io.v.syncbase.core.DatabaseHandle coreDatabaseHandle) {
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
        public String prefix = DEFAULT_COLLECTION_PREFIX;

        public CollectionOptions setWithoutSyncgroup(boolean value) {
            withoutSyncgroup = value;
            return this;
        }

        public CollectionOptions setPrefix(String value) {
            prefix = value;
            return this;
        }
    }

    /**
     * Creates a new collection and an associated syncgroup.
     * The id of the collection will include the creator's user id and its name will be a UUID.
     * Upon creation, both the collection and syncgroup are {@code READ_WRITE} for the creator.
     * Setting {@code opts.withoutSyncgroup} prevents syncgroup creation.
     * Setting {@code opts.prefix} will assign a UUID name starting with that prefix.
     * May only be called within a batch if {@code opts.withoutSyncgroup} is set.
     *
     * @param opts options for collection creation
     * @return the collection handle
     */
    public abstract Collection createCollection(CollectionOptions opts)
            throws SyncbaseException;

    /**
     * Calls {@code collection(name, opts)} with default {@code CollectionOptions}.
     */
    public Collection createCollection() throws SyncbaseException {
        return createCollection(new CollectionOptions());
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
    public Iterator<Collection> getCollections() throws SyncbaseException {
        try {

            ArrayList<Collection> collections = new ArrayList<>();
            for (io.v.syncbase.core.Id id : mCoreDatabaseHandle.listCollections()) {
                collections.add(getCollection(new Id(id)));
            }
            return collections.iterator();

        } catch (VError e) {
            chainThrow("getting collections in database", mCoreDatabaseHandle.id().name, e);
            throw new AssertionError("never happens");
        }
    }
}
