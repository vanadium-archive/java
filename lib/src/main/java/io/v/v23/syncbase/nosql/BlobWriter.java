// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import com.google.common.util.concurrent.ListenableFuture;

import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.nosql.BlobRef;

import java.io.OutputStream;

import javax.annotation.CheckReturnValue;

/**
 * Interface for writing data blobs.
 */
public interface BlobWriter {
    /**
     * Returns a reference to the blob being written.
     */
    BlobRef getRef();

    /**
     * Returns an {@link OutputStream} for writing data to this blob.
     * <p>
     * You should be aware of the following constraints on the returned {@link OutputStream}:
     * <p><ul>
     *     <li> it doesn't implement {@link OutputStream#flush}: the only way to ensure that the
     *          data is flushed is to call {@link OutputStream#close}.
     *     <li> if the blob is {@link #commit committed}, subsequent {@link OutputStream}
     *          {@link OutputStream#write writes} are guaranteed <strong>NOT</strong> to be applied
     *          to the blob; however, they might "succeed", i.e., they may not throw an
     *          {@link java.io.IOException}.  Any subsequent {@link OutputStream#close} operation
     *          on the {@link OutputStream} are guaranteed to fail immediately.
     *     <li> likewise, if the context used to create the {@link OutputStream} is
     *          {@link io.v.v23.context.CancelableVContext#cancel canceled}, subsequent
     *          {@link OutputStream} {@link OutputStream#write writes} are guaranteed
     *          <strong>NOT</strong> to be applied to the blob;  however, they might "succeed",
     *          i.e., they may not throw an {@link java.io.IOException}.  Any subsequent
     *          {@link OutputStream#close} operation on the {@link OutputStream} are guaranteed
     *          to fail immediately.
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
     * <p>
     * Please be aware that {@link OutputStream} operations are blocking.
     *
     * @param  ctx        vanadium context
     * @return            an {@link OutputStream} used for writing data to this blob
     */
    OutputStream stream(VContext ctx);

    /**
     * Marks the blob as immutable.
     *
     * @param  ctx        vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Void> commit(VContext ctx);

    /**
     * Deletes the blob locally (committed or uncommitted).
     * <p>
     * NOT YET IMPLEMENTED.
     *
     * @param ctx         vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Void> delete(VContext ctx);

    /**
     * Returns the {@link ListenableFuture} whose result is the size of the blob.
     * <p>
     * If the blob hasn't yet been {@link #commit commit}ed, returns the number of bytes
     * written so far.
     * <p>
     * This is the only accurate measure of a write progress: relying on a write progress of
     * {@link OutputStream} returned by {@link #stream} would be a mistake (see comments
     * on {@link #stream}).
     *
     * @param ctx         vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Long> size(VContext ctx);
}
