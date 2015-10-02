// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.context;

import io.v.v23.verror.VException;

/**
 * An extension of {@link VContext} interface that allows the user to explicitly cancel the context.
 */
public class CancelableVContext extends VContext {
    private long nativeCancelPtr;

    private native void nativeCancel(long nativeCancelPtr) throws VException;
    private native void nativeFinalize(long nativeCancelPtr);

    private CancelableVContext(long nativePtr, long nativeCancelPtr) {
        super(nativePtr);
        this.nativeCancelPtr = nativeCancelPtr;
    }

    /**
     * Cancels the context.  After this method is invoked, the counter returned by
     * {@link VContext#done done} method of the new context (and all contexts further derived
     * from it) will be set to zero.
     */
    public void cancel() {
        try {
            nativeCancel(nativeCancelPtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't cancel context", e);
        }
    }

    @Override
    protected void finalize() {
        super.finalize();
        nativeFinalize(nativeCancelPtr);
    }
}
