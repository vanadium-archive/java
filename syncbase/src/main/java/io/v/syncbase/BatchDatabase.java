// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import io.v.syncbase.core.VError;
import io.v.syncbase.exception.SyncbaseException;

import static io.v.syncbase.exception.Exceptions.chainThrow;

/**
 * Provides a way to perform a set of operations atomically on a database. See
 * {@code Database.beginBatch} for concurrency semantics.
 */
public class BatchDatabase extends DatabaseHandle {
    private final io.v.syncbase.core.BatchDatabase mCoreBatchDatabase;

    BatchDatabase(io.v.syncbase.core.BatchDatabase coreBatchDatabase) {
        super(coreBatchDatabase);
        mCoreBatchDatabase = coreBatchDatabase;
    }

    /**
     * @throws IllegalArgumentException if opts.withoutSyncgroup false
     */
    @Override
    public Collection collection(String name, CollectionOptions opts) throws SyncbaseException {
        if (!opts.withoutSyncgroup) {
            throw new IllegalArgumentException("Cannot create syncgroup in a batch");
        }
        Collection res = getCollection(new Id(Syncbase.getPersonalBlessingString(), name));
        res.createIfMissing();
        return res;
    }

    /**
     * Persists the pending changes to Syncbase. If the batch is read-only, {@code commit} will
     * throw {@code ConcurrentBatchException}; abort should be used instead.
     */
    public void commit() throws SyncbaseException {
        try {

            // TODO(sadovsky): Throw ConcurrentBatchException where appropriate.
            mCoreBatchDatabase.commit();

        } catch (VError e) {
            chainThrow("committing batch", e);
        }
    }

    /**
     * Notifies Syncbase that any pending changes can be discarded. Calling {@code abort} is not
     * strictly required, but may allow Syncbase to release locks or other resources sooner than if
     * {@code abort} was not called.
     */
    public void abort() throws SyncbaseException {
        try {

            mCoreBatchDatabase.abort();

        } catch (VError e) {
            chainThrow("aborting batch", e);
        }
    }
}
