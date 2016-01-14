// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import javax.annotation.CheckReturnValue;

import io.v.v23.VFutures;
import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.nosql.BatchOptions;
import io.v.v23.services.syncbase.nosql.ConcurrentBatchException;

/**
 * Various utility methods for the NoSql database.
 */
public class NoSql {
    /**
     * Creates a new nosql database given the parent name, database relative name, and a schema.
     */
    public static Database newDatabase(String parentFullName, String relativeName, Schema schema) {
        return DatabaseImpl.create(parentFullName, relativeName, schema);
    }

    /**
     * Interface for a batch operation that is executed as part of {@link #runInBatch runInBatch()}.
     */
    public interface BatchOperation {
        /**
         * Performs the batch operation.
         *
         * @param  db         batch database on which the operation is performed
         */
        @CheckReturnValue
        ListenableFuture<Void> run(BatchDatabase db);
    }

    /**
     * Runs the given batch operation, managing retries and
     * {@link BatchDatabase#commit commit()}/{@link BatchDatabase#abort abort()}s.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context    Vanadium context
     * @param  db         database on which the batch operation is to be performed
     * @param  opts       batch configuration
     * @param  op         batch operation
     */
    @CheckReturnValue
    public static ListenableFuture<Void> runInBatch(VContext context, Database db,
                                                    BatchOptions opts, BatchOperation op) {
        return VFutures.withUserLandChecks(context,
                Futures.transform(Futures.immediateFuture(false),
                        getRetryFn(context, db, opts, op, 0)));
    }

    private static AsyncFunction<Boolean, Void> getRetryFn(final VContext ctx,
                                                           final Database db,
                                                           final BatchOptions opts,
                                                           final BatchOperation op,
                                                           final int round) {
        return new AsyncFunction<Boolean, Void>() {
            @Override
            public ListenableFuture<Void> apply(Boolean success) throws Exception {
                if (success) {
                    return Futures.immediateFuture(null);
                }
                if (round >= 3) {
                    throw new ConcurrentBatchException(ctx);
                }
                return Futures.transform(tryBatch(ctx, db, opts, op),
                        getRetryFn(ctx, db, opts, op, round + 1));
            }
        };
    }

    @CheckReturnValue
    private static ListenableFuture<Boolean> tryBatch(final VContext ctx,
                                                      final Database db,
                                                      final BatchOptions opts,
                                                      final BatchOperation op) {
        final SettableFuture<Boolean> ret = SettableFuture.create();
        Futures.addCallback(db.beginBatch(ctx, opts), new FutureCallback<BatchDatabase>() {
            @Override
            public void onFailure(Throwable t) {
                ret.setException(t);
            }

            @Override
            public void onSuccess(final BatchDatabase batch) {
                Futures.addCallback(op.run(batch), new FutureCallback<Void>() {
                    @Override
                    public void onFailure(final Throwable t) {
                        Futures.addCallback(batch.abort(ctx), new FutureCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                ret.setException(t);
                            }

                            @Override
                            public void onFailure(Throwable newT) {
                                ret.setException(t);
                            }
                        });
                    }

                    @Override
                    public void onSuccess(Void result) {
                        Futures.addCallback(batch.commit(ctx), new FutureCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                ret.set(true);  // success
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                if (t instanceof ConcurrentBatchException) {
                                    // retry
                                    ret.set(false);
                                } else {
                                    ret.setException(t);
                                }
                            }
                        });
                    }
                });
            }
        });
        return ret;
    }

    private NoSql() {}
}
