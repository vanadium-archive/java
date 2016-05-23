// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import io.v.v23.VFutures;
import io.v.v23.security.access.Permissions;
import io.v.v23.verror.ExistException;
import io.v.v23.verror.VException;

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

    public Id getId() {
        return new Id(mVCollection.id());
    }

    // Shortcut for Database.getSyncgroup(c.getId()), helpful for the common case of one syncgroup
    // per collection.
    public Syncgroup getSyncgroup() {
        if (mDatabaseHandle instanceof BatchDatabase) {
            throw new RuntimeException("Must not call getSyncgroup within batch");
        }
        return ((Database) mDatabaseHandle).getSyncgroup(getId());
    }

    // TODO(sadovsky): Maybe add scan API, if developers aren't satisfied with watch.

    // TODO(sadovsky): Revisit this API. Is the Class<T> argument necessary?
    public <T> T get(String key, Class<T> cls) {
        try {
            return VFutures.sync(mVCollection.getRow(key).get(Syncbase.getVContext(), cls));
        } catch (VException e) {
            throw new RuntimeException("get failed: " + key, e);
        }
    }

    public boolean exists(String key) {
        try {
            return VFutures.sync(mVCollection.getRow(key).exists(Syncbase.getVContext()));
        } catch (VException e) {
            throw new RuntimeException("exists failed: " + key, e);
        }
    }

    public <T> void put(String key, T value) {
        try {
            VFutures.sync(mVCollection.put(Syncbase.getVContext(), key, value));
        } catch (VException e) {
            throw new RuntimeException("put failed: " + key, e);
        }
    }

    public void delete(String key) {
        try {
            VFutures.sync(mVCollection.getRow(key).delete(Syncbase.getVContext()));
        } catch (VException e) {
            throw new RuntimeException("delete failed: " + key, e);
        }
    }

    // FOR ADVANCED USERS. The following methods manipulate the AccessList for this collection, but
    // not for associated syncgroups.
    public AccessList getAccessList() {
        try {
            return new AccessList(VFutures.sync(mVCollection.getPermissions(Syncbase.getVContext())));
        } catch (VException e) {
            throw new RuntimeException("getPermissions failed", e);
        }
    }

    public void updateAccessList(final AccessList delta) {
        // Create a batch if we're not already in a batch.
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
        if (mDatabaseHandle instanceof BatchDatabase) {
            op.run((BatchDatabase) mDatabaseHandle);
        } else {
            ((Database) mDatabaseHandle).runInBatch(op, new Database.BatchOptions());
        }
    }
}
