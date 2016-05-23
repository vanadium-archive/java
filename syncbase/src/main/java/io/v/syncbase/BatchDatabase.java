// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import io.v.v23.VFutures;
import io.v.v23.verror.VException;

/**
 * Provides a way to perform a set of operations atomically on a database. See
 * {@code Database.beginBatch} for concurrency semantics.
 */
public class BatchDatabase extends DatabaseHandle {
    private final io.v.v23.syncbase.BatchDatabase mVBatchDatabase;

    protected BatchDatabase(io.v.v23.syncbase.BatchDatabase vBatchDatabase) {
        super(vBatchDatabase);
        mVBatchDatabase = vBatchDatabase;
    }

    @Override
    public Collection collection(String name, CollectionOptions opts) {
        if (!opts.withoutSyncgroup) {
            throw new RuntimeException("Cannot create syncgroup in a batch");
        }
        Collection res = getCollection(new Id(Syncbase.getPersonalBlessingString(), name));
        res.createIfMissing();
        return res;
    }

    /**
     * Persists the pending changes to Syncbase. If the batch is read-only, {@code commit} will
     * throw {@code ConcurrentBatchException}; abort should be used instead.
     */
    public void commit() {
        // TODO(sadovsky): Throw ConcurrentBatchException where appropriate.
        try {
            VFutures.sync(mVBatchDatabase.commit(Syncbase.getVContext()));
        } catch (VException e) {
            throw new RuntimeException("commit failed", e);
        }
    }

    /**
     * Notifies Syncbase that any pending changes can be discarded. Calling {@code abort} is not
     * strictly required, but may allow Syncbase to release locks or other resources sooner than if
     * {@code abort} was not called.
     */
    public void abort() {
        try {
            VFutures.sync(mVBatchDatabase.abort(Syncbase.getVContext()));
        } catch (VException e) {
            throw new RuntimeException("abort failed", e);
        }
    }
}
