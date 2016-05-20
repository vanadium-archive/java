// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.util.Iterator;

public class BatchDatabase implements DatabaseHandle {
    private final Database mDatabase;
    private final io.v.v23.syncbase.BatchDatabase mVBatchDatabase;

    protected BatchDatabase(Database database, io.v.v23.syncbase.BatchDatabase vBatchDatabase) {
        mDatabase = database;
        mVBatchDatabase = vBatchDatabase;
    }

    public Id getId() {
        return new Id(mVBatchDatabase.id());
    }

    public Collection collection(String name, CollectionOptions opts) {
        if (!opts.withoutSyncgroup) {
            throw new RuntimeException("Cannot create syncgroup in a batch");
        }
        return new Collection(mDatabase, mVBatchDatabase.getCollection(new io.v.v23.services.syncbase.Id(Syncbase.getPersonalBlessingString(), name)), true);
    }

    public Collection getCollection(Id id) {
        return Database.getCollectionImpl(mDatabase, mVBatchDatabase, id);
    }

    public Iterator<Collection> getCollections() {
        return Database.getCollectionsImpl(mDatabase, mVBatchDatabase);
    }

    public void commit() {
        mVBatchDatabase.commit(Syncbase.getVContext());
    }

    public void abort() {
        mVBatchDatabase.abort(Syncbase.getVContext());
    }
}
