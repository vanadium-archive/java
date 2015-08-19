// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.channel;

import com.google.common.collect.AbstractIterator;

import java.io.EOFException;
import java.util.Iterator;

/**
 * An implementation of {@link Iterable} that reads data from an underlying Go channel.
 */
public class ChannelIterable<T> implements Iterable<T> {
    private final long nativePtr;
    private final long sourceNativePtr;

    private native Object nativeReadValue(long nativePtr) throws EOFException;
    private native void nativeFinalize(long nativePtr, long sourceNativePtr);

    private ChannelIterable(long nativePtr, long sourceNativePtr) {
        this.nativePtr = nativePtr;
        this.sourceNativePtr = sourceNativePtr;
    }

    @Override
    public Iterator<T> iterator() {
        return new AbstractIterator<T>() {
            @Override
            protected T computeNext() {
                try {
                    return readValue();
                } catch (EOFException e) {
                    return endOfData();
                }
            }
        };
    }

    private T readValue() throws EOFException {
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
}