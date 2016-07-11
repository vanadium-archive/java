// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import io.v.syncbase.core.Permissions;
import io.v.syncbase.core.VError;
import io.v.syncbase.exception.SyncbaseException;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

import static io.v.syncbase.exception.Exceptions.chainThrow;

/**
 * Represents an ordered set of key-value pairs.
 * To get a Collection handle, call {@code Database.collection}.
 */
public class Collection {
    private final io.v.syncbase.core.Collection mCoreCollection;
    private final DatabaseHandle mDatabaseHandle;
    private final Id mId;

    Collection(io.v.syncbase.core.Collection coreCollection, DatabaseHandle databaseHandle) {
        mCoreCollection = coreCollection;
        mDatabaseHandle = databaseHandle;
        mId = new Id(coreCollection.id());
    }

    void createIfMissing() throws SyncbaseException {
        try {
            mCoreCollection.create(Syncbase.defaultCollectionPerms());
        } catch (VError vError) {
            if (vError.id.equals(VError.EXIST)) {
                return;
            }
            chainThrow("creating collection", vError);
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
     * @throws IllegalArgumentException if this is collection on a batch database.
     */
    public Syncgroup getSyncgroup() {
        if (mDatabaseHandle instanceof BatchDatabase) {
            throw new IllegalArgumentException("Must not call getSyncgroup within batch");
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
    public <T> T get(String key, Class<T> cls) throws SyncbaseException {
        try {
            return (T) VomUtil.decode(mCoreCollection.get(key), cls);
        } catch (VError vError) {
            if (vError.id.equals(VError.NO_EXIST)) {
                return null;
            }
            chainThrow("getting value from collection", mId, vError);
        } catch (VException e) {
            chainThrow("decoding value retrieved from collection", mId, e);
        }
        throw new AssertionError("never happens");
    }

    /**
     * Returns true if there is a value associated with {@code key}.
     */
    public boolean exists(String key) throws SyncbaseException {
        try {

            return mCoreCollection.row(key).exists();

        } catch (VError e) {
            chainThrow("checking if value exists in collection", mId, e);
            throw new AssertionError("never happens");
        }
    }

    /**
     * Puts {@code value} for {@code key}, overwriting any existing value. Idempotent.
     */
    public <T> void put(String key, T value) throws SyncbaseException {
        try {

            mCoreCollection.put(key, VomUtil.encode(value, value.getClass()));

        } catch (VError e) {
            chainThrow("putting value into collection", mId, e);
        } catch (VException e) {
            chainThrow("putting value into collection", mId, e);
        }
    }

    /**
     * Deletes the value associated with {@code key}. Idempotent.
     */
    public void delete(String key) throws SyncbaseException {
        try {

            mCoreCollection.delete(key);

        } catch (VError e) {
            chainThrow("deleting collection", mId, e);
        }
    }

    /**
     * FOR ADVANCED USERS. Returns the {@code AccessList} for this collection. Users should
     * typically manipulate access lists via {@code collection.getSyncgroup()}.
     */
    public AccessList getAccessList() throws SyncbaseException {
        try {

            return new AccessList(mCoreCollection.getPermissions());

        } catch (VError e) {
            chainThrow("getting access list of collection", mId, e);
            throw new AssertionError("never happens");
        }
    }

    /**
     * FOR ADVANCED USERS. Updates the {@code AccessList} for this collection. Users should
     * typically manipulate access lists via {@code collection.getSyncgroup()}.
     */
    public void updateAccessList(final AccessList delta) throws SyncbaseException {
        final Id id = this.getId();
        Database.BatchOperation op = new Database.BatchOperation() {
            @Override
            public void run(BatchDatabase db) throws SyncbaseException {
                io.v.syncbase.core.Collection coreCollection = db.getCollection(id)
                        .mCoreCollection;
                try {
                    Permissions newPermissions = AccessList.applyDeltaForCollection(
                            coreCollection.getPermissions(), delta);
                    coreCollection.setPermissions(newPermissions);
                } catch (VError vError) {
                    chainThrow("setting permissions in collection", id, vError);
                }
            }
        };
        // Create a batch if we're not already in a batch.
        if (mDatabaseHandle instanceof BatchDatabase) {
            op.run((BatchDatabase) mDatabaseHandle);
        } else {
            ((Database) mDatabaseHandle).runInBatch(op);
        }
    }
}
