// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import io.v.syncbase.core.VError;

/**
 * Provides a way to perform a set of operations atomically on a database. See
 * {@code Database.beginBatch} for concurrency semantics.
 */
public class BatchDatabase extends DatabaseHandle {
    protected io.v.syncbase.core.BatchDatabase mCoreBatchDatabase;

    protected BatchDatabase(io.v.syncbase.core.BatchDatabase coreBatchDatabase) {
        super(coreBatchDatabase);
        mCoreBatchDatabase = coreBatchDatabase;
    }

    @Override
    public Collection collection(String name, CollectionOptions opts) throws VError {
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
    public void commit() throws VError {
        // TODO(sadovsky): Throw ConcurrentBatchException where appropriate.
        mCoreBatchDatabase.commit();
    }

    /**
     * Notifies Syncbase that any pending changes can be discarded. Calling {@code abort} is not
     * strictly required, but may allow Syncbase to release locks or other resources sooner than if
     * {@code abort} was not called.
     */
    public void abort() throws VError {
        mCoreBatchDatabase.abort();
    }
}
