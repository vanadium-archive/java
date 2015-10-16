// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import io.v.v23.context.CancelableVContext;
import io.v.v23.vdl.TypedClientStream;
import io.v.v23.verror.Errors;
import io.v.v23.verror.VException;

import java.io.EOFException;
import java.util.Iterator;

/**
 * Implementation of the {@link Stream} interface that reads from a VDL stream.
 */
class StreamImpl<T> implements Stream<T> {
    private final CancelableVContext ctxC;
    private final TypedClientStream<Void, T, Void> stream;
    private volatile boolean isCanceled;
    private volatile boolean isCreated;

    StreamImpl(CancelableVContext ctxC, TypedClientStream<Void, T, Void> stream) {
        this.ctxC = ctxC;
        this.stream = stream;
        this.isCanceled = this.isCreated = false;
    }
    @Override
    public synchronized Iterator<T> iterator() {
        Preconditions.checkState(!isCreated, "Can only create one Stream iterator.");
        isCreated = true;
        return new AbstractIterator<T>() {
            @Override
            protected T computeNext() {
                synchronized (StreamImpl.this) {
                    if (isCanceled) {  // client canceled the stream
                        return endOfData();
                    }
                    try {
                        return stream.recv();
                    } catch (EOFException e) {  // legitimate end of stream
                        return endOfData();
                    } catch (VException e) {
                        if (isCanceled || Errors.CANCELED.getID().equals(e.getID())) {
                            return endOfData();
                        }
                        throw new RuntimeException("Error retrieving next stream element.", e);
                    }
                }
            }
        };
    }
    @Override
    public synchronized void cancel() throws VException {
        this.isCanceled = true;
        this.ctxC.cancel();
    }
}
