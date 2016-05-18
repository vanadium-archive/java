// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

public class Collection {
    public Id getId() {
        return null;
    }

    // Shortcut for Database.getSyncgroup(c.getId()), helpful for the common case of one syncgroup
    // per collection.
    public Syncgroup getSyncgroup() {
        return null;
    }

    // TODO(sadovsky): Maybe add scan API, if developers aren't satisfied with watch.
    public <T> T get(String key) {
        return null;
    }

    public boolean exists(String key) {
        return false;
    }

    public <T> void put(String key, T value) {

    }

    public void delete(String key) {

    }

    // FOR ADVANCED USERS. The following methods manipulate the AccessList for this collection, but
    // not for associated syncgroups.
    public AccessList getAccessList() {
        return null;
    }

    public void updateAccessList(AccessList delta) {

    }
}
