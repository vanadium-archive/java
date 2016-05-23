// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.v.v23.VFutures;
import io.v.v23.syncbase.DatabaseCore;
import io.v.v23.verror.VException;

public abstract class DatabaseHandle {
    protected DatabaseCore mVDatabaseCore;

    protected DatabaseHandle(DatabaseCore vDatabaseCore) {
        mVDatabaseCore = vDatabaseCore;
    }

    Id getId() {
        return new Id(mVDatabaseCore.id());
    }

    class CollectionOptions {
        public boolean withoutSyncgroup;
    }

    abstract Collection collection(String name, CollectionOptions opts);

    Collection collection(String name) {
        return collection(name, new CollectionOptions());
    }

    Collection getCollection(Id id) {
        // TODO(sadovsky): Consider throwing an exception or returning null if the collection does
        // not exist.
        return new Collection(mVDatabaseCore.getCollection(id.toVId()), this);
    }

    Iterator<Collection> getCollections() {
        List<io.v.v23.services.syncbase.Id> vIds;
        try {
            vIds = VFutures.sync(mVDatabaseCore.listCollections(Syncbase.getVContext()));
        } catch (VException e) {
            throw new RuntimeException("listCollections failed", e);
        }
        ArrayList<Collection> cxs = new ArrayList<>(vIds.size());
        for (io.v.v23.services.syncbase.Id vId : vIds) {
            cxs.add(new Collection(mVDatabaseCore.getCollection(vId), this));
        }
        return cxs.iterator();
    }
}
