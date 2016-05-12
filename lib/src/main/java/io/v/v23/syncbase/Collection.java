// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import com.google.common.util.concurrent.ListenableFuture;
import io.v.v23.InputChannel;
import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;
import io.v.v23.services.syncbase.Id;
import io.v.v23.services.syncbase.KeyValue;

import javax.annotation.CheckReturnValue;

/**
 * Interface for a database collection, i.e., a collection of {@link Row}s.
 */
public interface Collection {
    /**
     * Returns the id of this collection.
     */
    Id id();

    /**
     * Returns the full (i.e., object) name of this collection.
     */
    String fullName();

    /**
     * Creates this collection.
     * <p>
     * Must not be called from within a batch.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     * @param perms   collection permissions; if {@code null}, {@link Database}'s
     *                permissions are used
     */
    @CheckReturnValue
    ListenableFuture<Void> create(VContext context, Permissions perms);

    /**
     * Destroys this collection, permanently removing all of its data.
     * <p>
     * This method must not be called from within a batch.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Void> destroy(VContext context);

    /**
     * Returns a new {@link ListenableFuture} whose result is {@code true} iff this collection exists
     * and the caller has sufficient permissions to access it.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Boolean> exists(VContext context);

    /**
     * Returns a new {@link ListenableFuture} whose result are the current permissions for the
     * collection.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Permissions> getPermissions(VContext context);

    /**
     * Replaces the current permissions for the collection.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     * @param perms   new permissions for the collection
     */
    @CheckReturnValue
    ListenableFuture<Void> setPermissions(VContext context, Permissions perms);

    /**
     * Returns the row with the given primary key.
     * <p>
     * This is a non-blocking method.
     *
     * @param key primary key of the row
     */
    Row getRow(String key);

    /**
     * Returns a new {@link ListenableFuture} whose result is the value for the given primary key.
     * <p>
     * The returned future will fail if the row doesn't exist.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     * @param key     the primary key for a row
     * @param clazz   the Class to instantiate, populate during de-serialization, and return
     */
    @CheckReturnValue
    <T> ListenableFuture<T> get(VContext context, String key, Class<T> clazz);

    /**
     * Writes the value to the collection under the provided primary key.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     * @param key     primary key under which the value is to be written
     * @param value   value to be written
     */
    @CheckReturnValue
    ListenableFuture<Void> put(VContext context, String key, Object value);

    /**
     * Deletes the value for the given primary key.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     * @param key     primary key for the row to be deleted
     */
    @CheckReturnValue
    ListenableFuture<Void> delete(VContext context, String key);

    /**
     * Deletes all rows in the given half-open range {@code [start, limit)}. If {@code limit} is
     * {@code ""}, all rows with keys &ge; {@code start} are deleted.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     * @param range   range of rows to be deleted
     */
    @CheckReturnValue
    ListenableFuture<Void> deleteRange(VContext context, RowRange range);

    /**
     * Returns an {@link InputChannel} over all rows in the given half-open range
     * {@code [start, limit)}. If {@code limit} is {@code ""}, all rows with keys &ge; {@code start}
     * are included.
     * <p>
     * It is legal to perform writes concurrently with {@link #scan scan()}. The returned channel
     * reads from a consistent snapshot taken at the time of the method and will not reflect
     * subsequent writes to keys not yet reached by the stream.
     * <p>
     * {@link io.v.v23.context.VContext#cancel Canceling} the provided context will
     * stop the scan and cause the channel to stop producing elements.  Note that to
     * avoid memory leaks, the caller should drain the channel after cancelling the context.
     *
     * @param context Vanadium context
     * @param range   range of rows to be read
     * @return an {@link InputChannel} over all rows in the given half-open range
     * {@code [start, limit)}
     */
    InputChannel<KeyValue> scan(VContext context, RowRange range);

}
