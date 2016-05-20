// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

public class Syncgroup {
    private final Id mId;
    private final io.v.v23.syncbase.Syncgroup mVSyncgroup;

    // Note, we take 'id' because io.v.v23.syncbase.Syncgroup is missing a 'getId' method.
    protected Syncgroup(Id id, io.v.v23.syncbase.Syncgroup vSyncgroup) {
        mId = id;
        mVSyncgroup = vSyncgroup;
    }

    public Id getId() {
        return mId;
    }

    public AccessList getAccessList() {
        throw new RuntimeException("Not implemented");
    }

    public static class UpdateAccessListOptions {
        public boolean syncgroupOnly;
    }

    // The following methods update the AccessList for the syncgroup and its associated collections.
    // Setting opts.syncgroupOnly makes it so these methods only update the AccessList for the
    // syncgroup.
    public void addUsers(User[] users, AccessList.AccessLevel level, UpdateAccessListOptions opts) {
        AccessList delta = new AccessList();
        for (User u: users) {
            delta.users.put(u.getId(), level);
        }
        updateAccessList(delta, opts);
    }

    public void removeUsers(User[] users, UpdateAccessListOptions opts) {
        AccessList delta = new AccessList();
        for (User u: users) {
            delta.users.put(u.getId(), null);
        }
        updateAccessList(delta, opts);
    }

    // Applies 'delta' to the AccessList. Note, NULL enum means "remove".
    public void updateAccessList(AccessList delta, UpdateAccessListOptions opts) {
        throw new RuntimeException("Not implemented");
    }
}
