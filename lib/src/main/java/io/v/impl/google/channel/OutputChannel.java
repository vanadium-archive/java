// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.channel;

import io.v.v23.verror.VException;

/**
 * An implementation of {@link io.v.v23.OutputChannel} that writes to a Go channel.
 */
public class OutputChannel<T> implements io.v.v23.OutputChannel<T> {
    private final long nativePtr;

    private static native <T> void nativeWriteValue(long nativePtr, T value) throws VException;
    private static native <T> void nativeClose(long nativePtr) throws VException;
    private static native <T> void nativeFinalize(long nativePtr);

    private OutputChannel(long nativePtr) {
        this.nativePtr = nativePtr;
    }

    @Override
    public void writeValue(T value) throws VException {
        nativeWriteValue(nativePtr, value);
    }

    @Override
    public void close() throws VException {
        nativeClose(nativePtr);
    }

    @Override
    protected void finalize() throws Throwable {
        nativeFinalize(nativePtr);
    }
}
