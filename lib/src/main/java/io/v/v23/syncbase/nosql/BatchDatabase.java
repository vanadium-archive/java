// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase.nosql;

import com.google.common.util.concurrent.ListenableFuture;

import javax.annotation.CheckReturnValue;

import io.v.v23.context.VContext;

/**
 * A handle to a set of reads and writes to the database that should be considered an atomic unit.
 * <p>
 * See {@link Database#beginBatch Database.beginBatch()} for concurrency semantics.
 */
public interface BatchDatabase extends DatabaseCore {
    /**
     * Persists the pending changes to the database.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context    Vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Void> commit(VContext context);

    /**
     * Notifies the server that any pending changes can be discarded.
     * <p>
     * This method is not strictly required, but it may allow the server to release locks
     * or other resources sooner than if it was not called.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context    Vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Void> abort(VContext context);
}
