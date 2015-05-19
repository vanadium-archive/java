// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.namespace;

import io.v.v23.InputChannel;
import io.v.v23.context.VContext;
import io.v.v23.namespace.Namespace;
import io.v.v23.naming.GlobReply;
import io.v.v23.verror.VException;

/**
 * An implementation of {@link Namespace} that calls to native code for most
 * of its functionalities.
 */
public class NamespaceImpl implements Namespace {
    private final long nativePtr;

    private native InputChannel<GlobReply> nativeGlob(
        long nativePtr, VContext context, String pattern) throws VException;
    private native void nativeFinalize(long nativePtr);

    private NamespaceImpl(long nativePtr) {
        this.nativePtr = nativePtr;
    }
    @Override
    public InputChannel<GlobReply> glob(VContext context, String pattern) throws VException {
        return nativeGlob(this.nativePtr, context, pattern);
    }
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (this.getClass() != other.getClass()) return false;
        return this.nativePtr == ((NamespaceImpl) other).nativePtr;
    }
    @Override
    public int hashCode() {
        return Long.valueOf(this.nativePtr).hashCode();
    }
    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr);
    }
}
