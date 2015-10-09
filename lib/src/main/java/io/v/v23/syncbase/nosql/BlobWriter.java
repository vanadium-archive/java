// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.nosql.BlobRef;
import io.v.v23.verror.VException;

import java.io.OutputStream;

/**
 * Interface for writing data blobs.
 */
public interface BlobWriter {
    /**
     * Returns a reference to the blob being written.
     */
    BlobRef getRef();

    /**
     * Creates a new {@link OutputStream} for writing data to this blob.
     * <p>
     * You should be aware of the following constraints on the returned {@link OutputStream}:
     * <p><ul>
     *     <li> it doesn't implement {@link OutputStream#flush}: the only way to ensure that the
     *          data is flushed is to call {@link OutputStream#close}.
     *     <li> if the blob is {@link #commit committed}, subsequent {@link OutputStream}
     *          {@link OutputStream#write writes} are guaranteed {@strong NOT} to be applied to the
     *          blob; however, they might "succeed", i.e., they may not throw an
     *          {@link java.io.IOException}.  Any subsequent {@link OutputStream#close} operation
     *          on the {@link OutputStream} are guaranteed to fail immediately.
     *     <li> likewise, if the context used to create the {@link OutputStream} is
     *          {@link io.v.v23.context.CancelableVContext#cancel canceled}, subsequent
     *          {@link OutputStream} {@link OutputStream#write writes} are guaranteed {@strong NOT}
     *          to be applied to the blob;  however, they might "succeed", i.e., they may not throw
     *          an {@link java.io.IOException}.  Any subsequent {@link OutputStream#close} operation
     *          on the {@link OutputStream} are guaranteed to fail immediately.
     * </ul><p>
     *
     * If invoked on a committed blob, it is possible for this method to "succeed", i.e., to return
     * an {@link OutputStream}.  However, the first subsequent {@link OutputStream#close} invocation
     * on that {@link OutputStream} will fail.
     * <p>
     * This method may be called multiple times, e.g., to resume a failed write.  If resuming a
     * failed write, the most accurate measure of write progress is obtained by calling
     * {@link #size}: relying on a write progress of the returned {@link OutputStream} would be
     * a mistake (see comments about the {@link OutputStream#write write} constraints above).
     *
     * @param  ctx        vanadium context
     * @return            {@link OutputStream} used for writing data to this blob
     * @throws VException if the {@link OutputStream} couldn't be created
     */
    OutputStream stream(VContext ctx) throws VException;

    /**
     * Marks the blob as immutable.
     *
     * @param  ctx        vanadium context
     * @throws VException if the blob couldn't be marked immutable (e.g, it's already been
     *                    marked as such)
     */
    void commit(VContext ctx) throws VException;

    /**
     * Deletes the blob locally (committed or uncommitted).
     * <p>
     * NOT YET IMPLEMENTED.
     *
     * @param ctx         vanadium context
     * @throws VException if the blob couldn't be deleted
     */
    void delete(VContext ctx) throws VException;

    /**
     * Returns the size of the blob.
     * <p>
     * If the blob hasn't yet been {@link #commit commit}ed, returns the number of bytes
     * written so far.
     * <p>
     * This is the only accurate measure of a write progress: relying on a write progress of
     * {@link OutputStream} returned by {@link #stream} would be a mistake (see comments
     * on {@link #stream}).
     *
     * @param ctx         vanadium context
     * @throws VException if the blob size couldn't be determined
     */
    long size(VContext ctx) throws VException;
}
