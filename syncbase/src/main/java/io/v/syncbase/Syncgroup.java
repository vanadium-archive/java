// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.v.syncbase.core.SyncgroupMemberInfo;
import io.v.syncbase.core.SyncgroupSpec;
import io.v.syncbase.core.VError;
import io.v.syncbase.core.VersionedSyncgroupSpec;

/**
 * Represents a set of collections, synced amongst a set of users.
 * To get a Syncgroup handle, call {@code Database.syncgroup}.
 */
public class Syncgroup {
    private final Database mDatabase;
    private final io.v.syncbase.core.Syncgroup mCoreSyncgroup;

    Syncgroup(io.v.syncbase.core.Syncgroup coreSyncgroup, Database database) {
        mCoreSyncgroup = coreSyncgroup;
        mDatabase = database;
    }

    void createIfMissing(List<Collection> collections) throws VError {
        ArrayList<io.v.syncbase.core.Id> ids = new ArrayList<>();
        for (Collection cx : collections) {
            ids.add(cx.getId().toCoreId());
        }

        SyncgroupSpec spec = new SyncgroupSpec();
        spec.publishSyncbaseName = Syncbase.sOpts.getPublishSyncbaseName();
        spec.permissions = Syncbase.defaultSyncgroupPerms();
        spec.collections = ids;
        spec.mountTables = Syncbase.sOpts.mountPoints;
        spec.isPrivate = false;

        try {
            // TODO(razvanm): Figure out to what value we should set the sync priority in the
            // SyncgroupMemberInfo.
            mCoreSyncgroup.create(spec, new SyncgroupMemberInfo());
        } catch (VError vError) {
            if (vError.id.equals(VError.EXIST)) {
                // Syncgroup already exists.
                // TODO(sadovsky): Verify that the existing syncgroup has the specified
                // configuration, e.g., the specified collections? instead of returning early.
                return;
            }
            throw vError;
        }
    }

    protected void join() throws VError {
        // TODO(razvanm): Find a way to restrict the remote blessing. Cloud is one thing the remote
        // blessings should include.
        mCoreSyncgroup.join("", ImmutableList.of("..."), new SyncgroupMemberInfo());
    }

    /**
     * Returns the id of this syncgroup.
     */
    public Id getId() {
        return new Id(mCoreSyncgroup.getId());
    }

    /**
     * Returns the {@code AccessList} for this syncgroup.
     * Throws if the current user is not an admin of the syncgroup or its collection.
     */
    public AccessList getAccessList() throws VError {
        // TODO(alexfandrianto): Rework for advanced users.
        // We will not ask for the syncgroup spec. Instead, we will rely on the collection of this
        // syncgroup to have the correct permissions. There is an issue with not being able to
        // determine READ vs READ_WRITE from just the syncgroup spec because the write tag is only
        // available on the collection. This workaround will assume only a single collection per
        // syncgroup, which is why it might not succeed for advanced users.
        Id cId = new Id(mCoreSyncgroup.getSpec().syncgroupSpec.collections.get(0));
        return mDatabase.getCollection(cId).getAccessList();

        // return new AccessList(mCoreSyncgroup.getSpec().syncgroupSpec.permissions);
    }

    /**
     * FOR ADVANCED USERS. Configures the behavior of various {@code AccessList} manipulation
     * methods below.
     */
    public static class UpdateAccessListOptions {
        /**
         * If false (the default), the various {@code AccessList} manipulation methods update the
         * {@code AccessList} for the syncgroup and its associated collections. If true, these
         * methods only update the {@code AccessList} for the syncgroup.
         */
        public boolean syncgroupOnly;
    }

    /**
     * FOR ADVANCED USERS. Adds the given users to the syncgroup, with the specified access level.
     */
    public void inviteUsers(List<User> users, AccessList.AccessLevel level,
                            UpdateAccessListOptions opts) throws VError {
        AccessList delta = new AccessList();
        for (User u : users) {
            delta.setAccessLevel(u, level);
        }
        updateAccessList(delta, opts);
    }

    /**
     * Adds the given users to the syncgroup, with the specified access level.
     */
    public void inviteUsers(List<User> users, AccessList.AccessLevel level) throws VError {
        inviteUsers(users, level, new UpdateAccessListOptions());
    }

    /**
     * Adds the given user to the syncgroup, with the specified access level.
     */
    public void inviteUser(User user, AccessList.AccessLevel level) throws VError {
        inviteUsers(Collections.singletonList(user), level);
    }

    /**
     * FOR ADVANCED USERS. Removes the given users from the syncgroup.
     */
    public void ejectUsers(List<User> users, UpdateAccessListOptions opts) throws VError {
        AccessList delta = new AccessList();
        for (User u : users) {
            delta.removeAccessLevel(u);
        }
        updateAccessList(delta, opts);
    }

    /**
     * Removes the given users from the syncgroup.
     */
    public void ejectUsers(List<User> users) throws VError {
        ejectUsers(users, new UpdateAccessListOptions());
    }

    /**
     * Removes the given user from the syncgroup.
     */
    public void ejectUser(User user) throws VError {
        ejectUsers(Collections.singletonList(user));
    }

    /**
     * FOR ADVANCED USERS. Applies {@code delta} to the {@code AccessList}.
     */
    public void updateAccessList(final AccessList delta, UpdateAccessListOptions opts)
            throws VError {
        // TODO(sadovsky): Make it so SyncgroupSpec can be updated as part of a batch?
        VersionedSyncgroupSpec versionedSyncgroupSpec;
        try {
            versionedSyncgroupSpec = mCoreSyncgroup.getSpec();
        } catch (VError vError) {
            throw new RuntimeException("getSpec failed", vError);
        }
        versionedSyncgroupSpec.syncgroupSpec.permissions = AccessList.applyDeltaForSyncgroup(
                versionedSyncgroupSpec.syncgroupSpec.permissions, delta);
        mCoreSyncgroup.setSpec(versionedSyncgroupSpec);
        // TODO(sadovsky): There's a race here - it's possible for a collection to get destroyed
        // after getSpec() but before db.getCollection().
        final List<io.v.syncbase.core.Id> collectionsIds =
                versionedSyncgroupSpec.syncgroupSpec.collections;
        mDatabase.runInBatch(new Database.BatchOperation() {
            @Override
            public void run(BatchDatabase db) {
                for (io.v.syncbase.core.Id id : collectionsIds) {
                    try {
                        db.getCollection(new Id(id)).updateAccessList(delta);
                    } catch (VError vError) {
                        throw new RuntimeException("getCollection failed", vError);
                    }
                }
            }
        }, new Database.BatchOptions());
    }
}
