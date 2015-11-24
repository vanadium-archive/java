// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import com.google.common.util.concurrent.ListenableFuture;

import io.v.v23.VIterable;
import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.nosql.BlobFetchStatus;
import io.v.v23.services.syncbase.nosql.BlobRef;
import io.v.v23.verror.VException;

import java.io.InputStream;

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
     *
     * @param ctx         vanadium context
     * @throws VException if the blob size couldn't be determined
     */
    ListenableFuture<Long> size(VContext ctx);

    /**
     * Returns a new {@link ListenableFuture} whose result is the {@link InputStream} used for
     * reading the contents of the blob, starting at the given offset.
     * <p>
     * You should be aware of the following constraints on the returned {@link InputStream}:
     * <p><ul>
     *     <li> if the context used to create the {@link InputStream} is
     *          {@link io.v.v23.context.CancelableVContext#cancel canceled}, some of the
     *          subsequent {@link InputStream} {@link InputStream#read reads} may return valid
     *          values.  In fact, there is no hard guarantees that <strong>any</strong> subsequent
     *          reads will fail.
     * </ul><p>
     *
     * @param ctx         vanadium context
     * @param offset      offset at which to read the contents of the blob
     * @return            a new {@link ListenableFuture} whose result {@link InputStream} used for
     *                    reading the contents of the blob
     */
    ListenableFuture<InputStream> stream(VContext ctx, long offset);

    /**
     * Initiates a blob prefetch, i.e., copying the blob to a local cache.
     * <p>
     * The provided {@code priority} value controls the network priority of the blob.  Higher
     * priority blobs are prefetched before the lower priority ones.  However an ongoing blob
     * transfer is not interrupted.
     * <p>
     * Returns a new {@link ListenableFuture} whose result is the iterator that can be used to track
     * the progress of the prefetch.  When the iterator exhausts all of the iterable elements, the
     * blob is guaranteed to have been entirely copied to a local cache.
     * <p>
     * {@link io.v.v23.context.CancelableVContext#cancel Canceling} the provided context will
     * stop the prefetch and terminate the iterator early.
     *
     * @param ctx         vanadium context
     * @param priority    prefetch priority
     * @return            a {@link ListenableFuture} whose result is an iterator used for tracking
     *                    the progress of the prefetch
     */
    ListenableFuture<VIterable<BlobFetchStatus>> prefetch(VContext ctx, long priority);

    /**
     * Deletes the blob's local cached copy.
     * <p>
     * NOT YET IMPLEMENTED.
     *
     * @param ctx         vanadium context
     */
    ListenableFuture<Void> delete(VContext ctx);

    /**
     * Pins the blob to a local cache so that it is not evicted.
     * <p>
     * NOT YET IMPLEMENTED.
     *
     * @param ctx         vanadium context
     */
    ListenableFuture<Void> pin(VContext ctx);

    /**
     * Unpins the blob from the local cache so that it can be evicted if needed.
     * <p>
     * NOT YET IMPLEMENTED.
     *
     * @param ctx         vanadium context
     */
    ListenableFuture<Void> unpin(VContext ctx);

    /**
     * Sets the eviction rank for the blob in the local cache.  Lower-ranked blobs are more eagerly
     * evicted.
     * <p>
     * NOT YET IMPLEMENTED.
     *
     * @param ctx         vanadium context
     * @param rank        eviction rank
     */
    ListenableFuture<Void> keep(VContext ctx, long rank);
}