// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

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
     * Returns the size of the blob.
     *
     * @param ctx         vanadium context
     * @throws VException if the blob size couldn't be determined
     */
    long size(VContext ctx) throws VException;

    /**
     * Returns the {@link InputStream} used for reading the contents of the blob, starting at the
     * given offset.
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
     * @return            {@link InputStream} used for reading the contents of the blob
     * @throws VException if the stream couldn't be created (e.g., blob doesn't exist)
     */
    InputStream stream(VContext ctx, long offset) throws VException;

    /**
     * Initiates a blob prefetch, i.e., copying the blob to a local cache.
     * <p>
     * The provided {@code priority} value controls the network priority of the blob.  Higher
     * priority blobs are prefetched before the lower priority ones.  However an ongoing blob
     * transfer is not interrupted.
     * <p>
     * The return {@link Stream} can be used to track the progress of the prefetch.  When the
     * {@link Stream} exhausts all of the iterable elements, the blob is guaranteed to have
     * been entirely copied to a local cache.
     * <p>
     * This method doesn't block.
     *
     * @param ctx         vanadium context
     * @param priority    prefetch priority
     * @return            a stream used for tracking the progress of the prefetch
     * @throws VException if the blob couldn't be prefetched
     */
    Stream<BlobFetchStatus> prefetch(VContext ctx, long priority) throws VException;

    /**
     * Deletes the blob's local cached copy.
     * <p>
     * NOT YET IMPLEMENTED.
     *
     * @param ctx         vanadium context
     * @throws VException if the blob couldn't be deleted
     */
    void delete(VContext ctx) throws VException;

    /**
     * Pins the blob to a local cache so that it is not evicted.
     * <p>
     * NOT YET IMPLEMENTED.
     *
     * @param ctx         vanadium context
     * @throws VException if the blob couldn't be pinned
     */
    void pin(VContext ctx) throws VException;

    /**
     * Unpins the blob from the local cache so that it can be evicted if needed.
     * <p>
     * NOT YET IMPLEMENTED.
     *
     * @param ctx         vanadium context
     * @throws VException if the blob couldn't be un-pinned
     */
    void unpin(VContext ctx) throws VException;

    /**
     * Sets the eviction rank for the blob in the local cache.  Lower-ranked blobs are more eagerly
     * evicted.
     * <p>
     * NOT YET IMPLEMENTED.
     *
     * @param ctx         vanadium context
     * @param rank        eviction rank
     * @throws VException if the blob's local cache rank couldn't be set
     */
    void keep(VContext ctx, long rank) throws VException;
}