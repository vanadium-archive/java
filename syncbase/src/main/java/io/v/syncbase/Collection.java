// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import io.v.v23.VFutures;
import io.v.v23.security.access.Permissions;
import io.v.v23.verror.ExistException;
import io.v.v23.verror.NoExistException;
import io.v.v23.verror.VException;

/**
 * Represents an ordered set of key-value pairs.
 * To get a Collection handle, call {@code Database.collection}.
 */
public class Collection {
    private final io.v.v23.syncbase.Collection mVCollection;
    private final DatabaseHandle mDatabaseHandle;

    protected void createIfMissing() {
        try {
            VFutures.sync(mVCollection.create(Syncbase.getVContext(), Syncbase.defaultPerms()));
        } catch (ExistException e) {
            // Collection already exists.
        } catch (VException e) {
            throw new RuntimeException("Failed to create collection", e);
        }
    }

    protected Collection(io.v.v23.syncbase.Collection vCollection, DatabaseHandle databaseHandle) {
        mVCollection = vCollection;
        mDatabaseHandle = databaseHandle;
    }

    /**
     * Returns the id of this collection.
     */
    public Id getId() {
        return new Id(mVCollection.id());
    }

    /**
     * Shortcut for {@code Database.getSyncgroup(collection.getId())}, helpful for the common case
     * of one syncgroup per collection.
     */
    public Syncgroup getSyncgroup() {
        if (mDatabaseHandle instanceof BatchDatabase) {
            throw new RuntimeException("Must not call getSyncgroup within batch");
        }
        return ((Database) mDatabaseHandle).getSyncgroup(getId());
    }

    // TODO(sadovsky): Add deleteRange API.
    // TODO(sadovsky): Maybe add scan API, if developers aren't satisfied with watch.

    // TODO(sadovsky): Revisit the get API:
    // - Is the Class<T> argument necessary?
    // - Should we take the target Object as an argument, to avoid allocations?
    // - What should it do if there is no value for the given key? (Currently, it returns null.)

    /**
     * Returns the value associated with {@code key}.
     */
    public <T> T get(String key, Class<T> cls) {
        try {
            return VFutures.sync(mVCollection.getRow(key).get(Syncbase.getVContext(), cls));
        } catch (NoExistException e) {
            return null;
        } catch (VException e) {
            throw new RuntimeException("get failed: " + key, e);
        }
    }

    /**
     * Returns true if there is a value associated with {@code key}.
     */
    public boolean exists(String key) {
        try {
            return VFutures.sync(mVCollection.getRow(key).exists(Syncbase.getVContext()));
        } catch (VException e) {
            throw new RuntimeException("exists failed: " + key, e);
        }
    }

    /**
     * Puts {@code value} for {@code key}, overwriting any existing value. Idempotent.
     */
    public <T> void put(String key, T value) {
        try {
            VFutures.sync(mVCollection.put(Syncbase.getVContext(), key, value));
        } catch (VException e) {
            throw new RuntimeException("put failed: " + key, e);
        }
    }

    /**
     * Deletes the value associated with {@code key}. Idempotent.
     */
    public void delete(String key) {
        try {
            VFutures.sync(mVCollection.getRow(key).delete(Syncbase.getVContext()));
        } catch (VException e) {
            throw new RuntimeException("delete failed: " + key, e);
        }
    }

    /**
     * FOR ADVANCED USERS. Returns the {@code AccessList} for this collection. Users should
     * typically manipulate access lists via {@code collection.getSyncgroup()}.
     */
    public AccessList getAccessList() {
        try {
            return new AccessList(VFutures.sync(mVCollection.getPermissions(Syncbase.getVContext())));
        } catch (VException e) {
            throw new RuntimeException("getPermissions failed", e);
        }
    }

    /**
     * FOR ADVANCED USERS. Updates the {@code AccessList} for this collection. Users should
     * typically manipulate access lists via {@code collection.getSyncgroup()}.
     */
    public void updateAccessList(final AccessList delta) {
        final Id id = this.getId();
        Database.BatchOperation op = new Database.BatchOperation() {
            @Override
            public void run(BatchDatabase db) {
                io.v.v23.syncbase.Collection vCx = db.getCollection(id).mVCollection;
                Permissions perms;
                try {
                    perms = VFutures.sync(vCx.getPermissions(Syncbase.getVContext()));
                } catch (VException e) {
                    throw new RuntimeException("getPermissions failed", e);
                }
                AccessList.applyDelta(perms, delta);
                try {
                    VFutures.sync(vCx.setPermissions(Syncbase.getVContext(), perms));
                } catch (VException e) {
                    throw new RuntimeException("setPermissions failed", e);
                }
            }
        };
        // Create a batch if we're not already in a batch.
        if (mDatabaseHandle instanceof BatchDatabase) {
            op.run((BatchDatabase) mDatabaseHandle);
        } else {
            ((Database) mDatabaseHandle).runInBatch(op, new Database.BatchOptions());
        }
    }
}
