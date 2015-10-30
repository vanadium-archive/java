// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.channel;

import com.google.common.collect.AbstractIterator;
import io.v.v23.VIterable;
import io.v.v23.verror.VException;

import java.io.EOFException;
import java.util.Iterator;

/**
 * An implementation of {@link VIterable} that reads data from an underlying Go channel.
 */
class ChannelIterable<T> implements VIterable<T> {
    private final long nativeValuePtr;
    private final long nativeErrorPtr;
    private final long nativeSourcePtr;
    private volatile VException error;

    private native Object nativeReadValue(long nativeValuePtr, long nativeErrorPtr)
            throws EOFException, VException;
    private native void nativeFinalize(
            long nativeValuePtr, long nativeErrorPtr, long nativeSourcePtr);

    private ChannelIterable(long nativeValuePtr, long nativeErrorPtr, long nativeSourcePtr) {
        this.nativeValuePtr = nativeValuePtr;
        this.nativeErrorPtr = nativeErrorPtr;
        this.nativeSourcePtr = nativeSourcePtr;
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
                } catch (VException e) {
                    error = e;
                    return endOfData();
                }
            }
        };
    }

    @Override
    public VException error() {
        return error;
    }

    private T readValue() throws EOFException, VException {
        return (T) nativeReadValue(nativeValuePtr, nativeErrorPtr);
    }

    @Override
    protected void finalize() {
        nativeFinalize(nativeValuePtr, nativeErrorPtr, nativeSourcePtr);
    }
}