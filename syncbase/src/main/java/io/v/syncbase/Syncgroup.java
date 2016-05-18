// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

public class Syncgroup {
    public Id getId() {
        return null;
    }

    public AccessList getAccessList() {
        return null;
    }

    public class UpdateAccessListOptions {
        // TODO(sadovsky): Fill this in.
    }

    // The following methods update the AccessList for the syncgroup and its associated collections.
    // Setting opts.syncgroupOnly makes it so these methods only update the AccessList for the
    // syncgroup.
    public void addUsers(User[] users, AccessLevel level, UpdateAccessListOptions opts) {

    }

    public void removeUsers(User[] users, UpdateAccessListOptions opts) {

    }

    // Applies 'delta' to the AccessList. Note, NULL enum means "remove".
    public void updateAccessList(AccessList delta, UpdateAccessListOptions opts) {

    }
}
