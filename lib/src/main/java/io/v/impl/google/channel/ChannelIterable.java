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
    private final long nativeChanPtr;
    private final long nativeConvertPtr;
    private VException error;

    private native Object nativeReadValue(long nativeChanPtr, long nativeConvertPtr)
            throws EOFException, VException;
    private native void nativeFinalize(long nativeChanPtr, long nativeConvertPtr);

    private ChannelIterable(long nativeChanPtr, long nativeConvertPtr) {
        this.nativeChanPtr = nativeChanPtr;
        this.nativeConvertPtr = nativeConvertPtr;
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
        return (T) nativeReadValue(nativeChanPtr, nativeConvertPtr);
    }

    @Override
    protected void finalize() {
        nativeFinalize(nativeChanPtr, nativeConvertPtr);
    }
}