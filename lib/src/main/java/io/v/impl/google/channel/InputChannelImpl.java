// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.channel;

import com.google.common.collect.AbstractIterator;

import io.v.v23.InputChannel;
import io.v.v23.verror.VException;

import java.io.EOFException;
import java.util.Iterator;

/**
 * An implementation of {@link InputChannel} that calls to native code for most
 * of its functionalities.
 */
public class InputChannelImpl<T> implements InputChannel<T> {
    private final long nativePtr;
    private final long sourceNativePtr;

    private native boolean nativeAvailable(long nativePtr);
    private native Object nativeReadValue(long nativePtr) throws EOFException, VException;
    private native void nativeFinalize(long nativePtr, long sourceNativePtr);

    private InputChannelImpl(long nativePtr, long sourceNativePtr) {
        this.nativePtr = nativePtr;
        this.sourceNativePtr = sourceNativePtr;
    }

    @Override
    public boolean available() {
        return nativeAvailable(this.nativePtr);
    }

    @SuppressWarnings("unchecked")
    public T readValue() throws EOFException, VException {
        return (T) nativeReadValue(this.nativePtr);
    }
    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr, this.sourceNativePtr);
    }
    /**
     * Returns the native pointer to the Go channel of Java objects.
     *
     * @return the native pointer to the Go channel of Java objects
     */
    public long getNativePtr() { return this.nativePtr; }

    /**
     * Returns the native pointer to the Go channel that feeds the above Go channel of Java object.
     *
     * @return the native pointer to the Go channel that feeds the above Go channel of Java object
     */
    public long getSourceNativePtr() { return this.sourceNativePtr; }

    @Override
    public Iterator<T> iterator() {
        return new AbstractIterator<T>() {
            @Override
            protected T computeNext() {
                try {
                    return readValue();
                } catch (EOFException e) {
                    return endOfData();
                } catch (VException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }
}