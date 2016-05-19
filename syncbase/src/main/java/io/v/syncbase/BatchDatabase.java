// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.util.Iterator;

public class BatchDatabase implements DatabaseHandle {
    private final io.v.v23.syncbase.BatchDatabase mBatchDbImpl;

    protected BatchDatabase(io.v.v23.syncbase.BatchDatabase batchDbImpl) {
        mBatchDbImpl = batchDbImpl;
    }

    public Id getId() {
        return new Id(mBatchDbImpl.id());
    }

    public Collection collection(String name, CollectionOptions opts) {
        if (!opts.withoutSyncgroup) {
            throw new RuntimeException("Cannot create syncgroup in a batch");
        }
        return new Collection(mBatchDbImpl.getCollection(new io.v.v23.services.syncbase.Id(Syncbase.getPersonalBlessingString(), name)), true);
    }

    public Collection getCollection(Id id) {
        return Database.getCollectionImpl(mBatchDbImpl, id);
    }

    public Iterator<Collection> getCollections() {
        return Database.getCollectionsImpl(mBatchDbImpl);
    }

    public void commit() {
        mBatchDbImpl.commit(Syncbase.getVContext());
    }

    public void abort() {
        mBatchDbImpl.abort(Syncbase.getVContext());
    }
}
