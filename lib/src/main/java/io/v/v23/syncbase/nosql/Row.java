// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase.nosql;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.reflect.Type;

import javax.annotation.CheckReturnValue;

import io.v.v23.context.VContext;

/**
 * A handle for a single row in a {@link Table}.
 */
public interface Row {
    /**
     * Returns the primary key for this row.
     */
    String key();

    /**
     * Returns the full (i.e., object) name of this row.
     */
    String fullName();

    /**
     * Returns a new {@link ListenableFuture} whose result is {@code true} iff this row exists and
     * the caller has permissions to access it.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context        Vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Boolean> exists(VContext context);

    /**
     * Deletes this row.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context        Vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Void> delete(VContext context);

    /**
     * Returns the value for this row.
     * <p>
     * The returned {@link ListenableFuture} will fail if the row doesn't exist.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context        Vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Object> get(VContext context, Type type);

    /**
     * Writes the given value for this row.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context    Vanadium context
     * @param  value      value to write
     */
    @CheckReturnValue
    ListenableFuture<Void> put(VContext context, Object value, Type type);
}