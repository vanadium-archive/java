// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import com.google.common.util.concurrent.ListenableFuture;

import io.v.v23.InputChannel;
import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.nosql.BlobFetchStatus;
import io.v.v23.services.syncbase.nosql.BlobRef;

import java.io.InputStream;

import javax.annotation.CheckReturnValue;

/**
 * Interface for reading an (immutable) data blob.
 */
public interface BlobReader {
    /**
     * Returns a reference to the blob being read.
     */
    BlobRef getRef();

    /**
     * Returns a new {@link ListenableFuture} whose result is the size of the blob.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     */
    @CheckReturnValue
    ListenableFuture<Long> size(VContext context);

    /**
     * Returns an {@link InputStream} used for reading the contents of the blob, starting at the
     * given offset.
     * <p>
     * You should be aware of the following constraints on the returned {@link InputStream}:
     * <p><ul>
     *     <li> if the context used to create the {@link InputStream} is
     *          {@link io.v.v23.context.VContext#cancel canceled}, some of the
     *          subsequent {@link InputStream} {@link InputStream#read reads} may return valid
     *          values.  In fact, there is no hard guarantees that <strong>any</strong> subsequent
     *          reads will fail.
     * </ul><p>
     * Please be aware that {@link InputStream} operations are blocking.
     *
     * @param context     vanadium context
     * @param offset      offset at which to read the contents of the blob
     * @return            an {@link InputStream} used for reading the contents of the blob
     */
    InputStream stream(VContext context, long offset);

    /**
     * Initiates a blob prefetch, i.e., copying the blob to a local cache.
     * <p>
     * The provided {@code priority} value controls the network priority of the blob.  Higher
     * priority blobs are prefetched before the lower priority ones.  However an ongoing blob
     * transfer is not interrupted.
     * <p>
     * Returns an {@link InputChannel} that can be used to track the progress of the prefetch.
     * When the iterator exhausts all of the iterable elements, the blob is guaranteed to have been
     * entirely copied to a local cache.
     * <p>
     * {@link io.v.v23.context.VContext#cancel Canceling} the provided context will
     * stop the prefetch and cause the channel to stop producing elements.  Note that to avoid
     * memory leaks, the caller should drain the channel after cancelling the context.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context     vanadium context
     * @param priority    prefetch priority
     * @return            an {@link InputChannel} that can be used to track the progress of the
     *                    prefetch
     */
    InputChannel<BlobFetchStatus> prefetch(VContext context, long priority);

    /**
     * Deletes the blob's local cached copy.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     * <p>
     * NOT YET IMPLEMENTED.
     *
     * @param context         vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Void> delete(VContext context);

    /**
     * Pins the blob to a local cache so that it is not evicted.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     * <p>
     * NOT YET IMPLEMENTED.
     *
     * @param context         vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Void> pin(VContext context);

    /**
     * Unpins the blob from the local cache so that it can be evicted if needed.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     * <p>
     * NOT YET IMPLEMENTED.
     *
     * @param context         vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Void> unpin(VContext context);

    /**
     * Sets the eviction rank for the blob in the local cache.  Lower-ranked blobs are more eagerly
     * evicted.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     * <p>
     * NOT YET IMPLEMENTED.
     *
     * @param context     vanadium context
     * @param rank        eviction rank
     */
    @CheckReturnValue
    ListenableFuture<Void> keep(VContext context, long rank);
}