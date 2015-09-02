// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import com.google.common.collect.AbstractIterator;

import java.io.EOFException;
import java.util.Iterator;

import io.v.v23.context.CancelableVContext;
import io.v.v23.services.syncbase.nosql.KeyValue;
import io.v.v23.vdl.TypedClientStream;
import io.v.v23.verror.VException;

class ScanStreamImpl implements ScanStream {
    private final CancelableVContext ctxC;
    private final TypedClientStream<Void, KeyValue, Void> stream;
    private volatile boolean isCanceled;
    private volatile boolean isCreated;

    ScanStreamImpl(CancelableVContext ctxC, TypedClientStream<Void, KeyValue, Void> stream) {
        this.ctxC = ctxC;
        this.stream = stream;
        this.isCanceled = this.isCreated = false;
    }
    // Implements Iterable.
    @Override
    public synchronized Iterator<KeyValue> iterator() {
        if (this.isCreated) {
            throw new RuntimeException("Can only create one ScanStream iterator.");
        }
        this.isCreated = true;
        return new AbstractIterator<KeyValue>() {
            @Override
            protected KeyValue computeNext() {
                synchronized (ScanStreamImpl.this) {
                    if (ScanStreamImpl.this.isCanceled) {  // client canceled the stream
                        return endOfData();
                    }
                    try {
                        return ScanStreamImpl.this.stream.recv();
                    } catch (EOFException e) {  // legitimate end of stream
                        return endOfData();
                    } catch (VException e) {
                        throw new RuntimeException("Error retrieving next stream element.", e);
                    }
                }
            }
        };
    }

    // Implements ScanStream.
    @Override
    public synchronized void cancel() throws VException {
        this.isCanceled = true;
        this.stream.finish();
    }
}