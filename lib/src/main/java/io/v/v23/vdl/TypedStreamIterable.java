// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.vdl;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import io.v.v23.VIterable;
import io.v.v23.verror.Errors;
import io.v.v23.verror.VException;

import java.io.EOFException;
import java.util.Iterator;

/**
 * Implementation of {@link VIterable} that reads from a {@link TypedClientStream}.
 */
public class TypedStreamIterable<T> implements VIterable<T> {
    private final TypedClientStream<Void, T, Void> stream;
    private volatile boolean isCreated;
    protected VException error;

    public TypedStreamIterable(TypedClientStream<Void, T, Void> stream) {
        this.stream = stream;
    }
    @Override
    public synchronized Iterator<T> iterator() {
        Preconditions.checkState(!isCreated, "Can only create one Stream iterator.");
        isCreated = true;
        return new AbstractIterator<T>() {
            @Override
            protected T computeNext() {
                synchronized (TypedStreamIterable.this) {
                    try {
                        return stream.recv();
                    } catch (EOFException e) {  // legitimate end of stream
                        return endOfData();
                    } catch (VException e) {
                        if (!Errors.CANCELED.getID().equals(e.getID())) {  // stream not canceled
                            error = e;
                        }
                        return endOfData();
                    }
                }
            }
        };
    }

    @Override
    public VException error() {
        return error;
    }
}
