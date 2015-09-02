// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import com.google.common.collect.AbstractIterator;

import java.io.EOFException;
import java.util.Iterator;
import java.util.List;

import io.v.v23.context.CancelableVContext;
import io.v.v23.vdl.TypedClientStream;
import io.v.v23.vdl.VdlAny;
import io.v.v23.verror.VException;

class ResultStreamImpl implements ResultStream {
    private final CancelableVContext ctxC;
    private final TypedClientStream<Void, List<VdlAny>, Void> stream;
    private final List<String> columnNames;
    private volatile boolean isCanceled;
    private volatile boolean isCreated;

    ResultStreamImpl(CancelableVContext ctxC, TypedClientStream<Void, List<VdlAny>, Void> stream,
            List<String> columnNames) {
        this.ctxC = ctxC;
        this.stream = stream;
        this.columnNames = columnNames;
        this.isCanceled = this.isCreated = false;
    }
    // Implements Iterable.
    @Override
    public synchronized Iterator<List<VdlAny>> iterator() {
        if (this.isCreated) {
            throw new RuntimeException("Can only create one ResultStream iterator.");
        }
        this.isCreated = true;
        return new AbstractIterator<List<VdlAny>>() {
            @Override
            protected List<VdlAny> computeNext() {
                synchronized (ResultStreamImpl.this) {
                    if (ResultStreamImpl.this.isCanceled) {  // client canceled the stream
                        return endOfData();
                    }
                    try {
                        return ResultStreamImpl.this.stream.recv();
                    } catch (EOFException e) {  // legitimate end of stream
                        return endOfData();
                    } catch (VException e) {
                        throw new RuntimeException("Error retrieving next stream element.", e);
                    }
                }
            }
        };
    }

    // Implements ResultStream.
    @Override
    public List<String> columnNames() {
        return this.columnNames;
    }
    @Override
    public synchronized void cancel() throws VException {
        this.isCanceled = true;
        this.stream.finish();
    }
}
