// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import io.v.v23.VFutures;
import io.v.v23.verror.ExistException;
import io.v.v23.verror.VException;

public class Collection {
    private final io.v.v23.syncbase.Collection mCxImpl;

    protected Collection(io.v.v23.syncbase.Collection cxImpl, boolean createIfMissing) {
        if (createIfMissing) {
            try {
                // TODO(sadovsky): Revisit these default perms, which were copied from the Todos app.
                VFutures.sync(cxImpl.create(Syncbase.getVContext(), null));
            } catch (ExistException e) {
                // Collection already exists.
            } catch (VException e) {
                throw new RuntimeException("Failed to create collection", e);
            }
        }
        mCxImpl = cxImpl;
    }

    public Id getId() {
        return new Id(mCxImpl.id());
    }

    // Shortcut for Database.getSyncgroup(c.getId()), helpful for the common case of one syncgroup
    // per collection.
    public Syncgroup getSyncgroup() {
        throw new RuntimeException("Not implemented");
    }

    // TODO(sadovsky): Maybe add scan API, if developers aren't satisfied with watch.
    public <T> T get(String key) {
        throw new RuntimeException("Not implemented");
    }

    public boolean exists(String key) {
        throw new RuntimeException("Not implemented");
    }

    public <T> void put(String key, T value) {
        throw new RuntimeException("Not implemented");
    }

    public void delete(String key) {
        throw new RuntimeException("Not implemented");
    }

    // FOR ADVANCED USERS. The following methods manipulate the AccessList for this collection, but
    // not for associated syncgroups.
    public AccessList getAccessList() {
        throw new RuntimeException("Not implemented");
    }

    public void updateAccessList(AccessList delta) {
        throw new RuntimeException("Not implemented");
    }
}
