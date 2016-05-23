// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

public class BatchDatabase extends DatabaseHandle {
    private final io.v.v23.syncbase.BatchDatabase mVBatchDatabase;

    protected BatchDatabase(io.v.v23.syncbase.BatchDatabase vBatchDatabase) {
        super(vBatchDatabase);
        mVBatchDatabase = vBatchDatabase;
    }

    public Collection collection(String name, CollectionOptions opts) {
        if (!opts.withoutSyncgroup) {
            throw new RuntimeException("Cannot create syncgroup in a batch");
        }
        Collection res = getCollection(new Id(Syncbase.getPersonalBlessingString(), name));
        res.createIfMissing();
        return res;
    }

    public void commit() {
        mVBatchDatabase.commit(Syncbase.getVContext());
    }

    public void abort() {
        mVBatchDatabase.abort(Syncbase.getVContext());
    }
}
