// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.nosql.BatchOptions;
import io.v.v23.services.syncbase.nosql.Errors;
import io.v.v23.verror.VException;

/**
 * Various utility methods for the NoSql database.
 */
public class NoSql {
    public static Database newDatabase(String parentFullName, String relativeName, Schema schema) {
        return new DatabaseImpl(parentFullName, relativeName, "", schema);
    }

    /**
     * Interface for a batch operation that is executed as part of {@link #runInBatch runInBatch()}.
     */
    public static interface BatchOperation {
        /**
         * Performs the batch operation.
         *
         * @param  db         batch database on which the operation is performed
         * @throws VException if there was an error performing the operation;  if thrown, the
         *                    batch operation is aborted
         */
        void run(BatchDatabase db) throws VException;
    }

    /**
     * Runs the given batch operation, managing retries and
     * {@link BatchDatabase#commit commit()}/{@link BatchDatabase#abort abort()}s.
     *
     * @param  ctx        Vanadium context
     * @param  db         database on which the batch operation is to be performed
     * @param  opts       batch configuration
     * @param  op         batch operation
     * @throws VException if there was an error executing the given batch operation
     */
    public static void runInBatch(VContext ctx, Database db, BatchOptions opts, BatchOperation op)
            throws VException {
        for (int i = 0; i < 3; ++i) {
            BatchDatabase batch = db.beginBatch(ctx, opts);
            try {
                op.run(batch);
            } catch (VException e) {
                batch.abort(ctx);
                throw e;
            }
            try {
                batch.commit(ctx);
                return;
            } catch (VException e) {
                if (!e.getID().equals(Errors.CONCURRENT_BATCH)) {
                    throw e;
                }
            }
        }
        throw Errors.newConcurrentBatch(ctx);
    }

    private NoSql() {}
}
