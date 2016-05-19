// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

public class Syncgroup {
    public Id getId() {
        throw new RuntimeException("Not implemented");
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
    public void addUsers(User[] users, AccessLevel level, UpdateAccessListOptions opts) {
        throw new RuntimeException("Not implemented");
    }

    public void removeUsers(User[] users, UpdateAccessListOptions opts) {
        throw new RuntimeException("Not implemented");
    }

    // Applies 'delta' to the AccessList. Note, NULL enum means "remove".
    public void updateAccessList(AccessList delta, UpdateAccessListOptions opts) {
        throw new RuntimeException("Not implemented");
    }
}
