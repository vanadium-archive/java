// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import io.v.syncbase.core.Permissions;
import io.v.syncbase.core.VError;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * Represents an ordered set of key-value pairs.
 * To get a Collection handle, call {@code Database.collection}.
 */
public class Collection {
    private final io.v.syncbase.core.Collection mCoreCollection;
    private final DatabaseHandle mDatabaseHandle;
    private final Id mId;

    protected Collection(io.v.syncbase.core.Collection coreCollection, DatabaseHandle databaseHandle) {
        mCoreCollection = coreCollection;
        mDatabaseHandle = databaseHandle;
        mId = new Id(coreCollection.id());
    }

    protected void createIfMissing() {
        try {
            mCoreCollection.create(Syncbase.defaultCollectionPerms());
        } catch (VError vError) {
            if (vError.id.equals(VError.EXIST)) {
                return;
            }
            throw new RuntimeException("Failed to create collection", vError);
        }
    }

    /**
     * Returns the id of this collection.
     */
    public Id getId() {
        return mId;
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
    public <T> T get(String key, Class<T> cls) throws VError {
        try {
            return (T) VomUtil.decode(mCoreCollection.get(key), cls);
        } catch (VError vError) {
            if (vError.id.equals(VError.NO_EXIST)) {
                return null;
            }
            throw vError;
        } catch (VException e) {
            throw new VError(e);
        }
    }

    /**
     * Returns true if there is a value associated with {@code key}.
     */
    public boolean exists(String key) throws VError {
        return mCoreCollection.row(key).exists();
    }

    /**
     * Puts {@code value} for {@code key}, overwriting any existing value. Idempotent.
     */
    public <T> void put(String key, T value) throws VError {
        try {
            mCoreCollection.put(key, VomUtil.encode(value, value.getClass()));
        } catch (VException e) {
            throw new VError(e);
        }
    }

    /**
     * Deletes the value associated with {@code key}. Idempotent.
     */
    public void delete(String key) throws VError {
        mCoreCollection.delete(key);
    }

    /**
     * FOR ADVANCED USERS. Returns the {@code AccessList} for this collection. Users should
     * typically manipulate access lists via {@code collection.getSyncgroup()}.
     */
    public AccessList getAccessList() throws VError {
        return new AccessList(mCoreCollection.getPermissions());
    }

    /**
     * FOR ADVANCED USERS. Updates the {@code AccessList} for this collection. Users should
     * typically manipulate access lists via {@code collection.getSyncgroup()}.
     */
    public void updateAccessList(final AccessList delta) throws VError {
        final Id id = this.getId();
        Database.BatchOperation op = new Database.BatchOperation() {
            @Override
            public void run(BatchDatabase db) {
                io.v.syncbase.core.Collection coreCollection = db.getCollection(id).mCoreCollection;
                try {
                    Permissions newPermissions = AccessList.applyDelta(
                            coreCollection.getPermissions(), delta);
                    coreCollection.setPermissions(newPermissions);
                } catch (VError vError) {
                    throw new RuntimeException("updateAccessList failed", vError);
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
