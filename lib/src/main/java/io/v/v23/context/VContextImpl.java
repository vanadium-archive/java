// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.context;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import io.v.v23.verror.VException;

import java.util.concurrent.CountDownLatch;

public class VContextImpl extends CancelableVContext {
    private static native CancelableVContext nativeCreate() throws VException;

    /**
     * Creates a new context with no data attached.
     *
     * This function is meant for use in tests only - the preferred way of obtaining a fully
     * initialized context is through the Vanadium runtime.
     *
     * @return a new root context with no data attached
     */
    public static CancelableVContext create() {
        try {
            return nativeCreate();
        } catch (VException e) {
            throw new RuntimeException("Couldn't create new context", e);
        }
    }

    private long nativePtr;
    private long nativeCancelPtr;  // zero for non-cancelable contexts.
    // Cached "done()" CountDownLatch, as we're supposed to return the same object on every call.
    private volatile CountDownLatch doneLatch = null;

    private native DateTime nativeDeadline(long nativePtr) throws VException;
    private native CountDownLatch nativeDone(long nativePtr) throws VException;
    private native Object nativeValue(long nativePtr, Object key) throws VException;
    private native CancelableVContext nativeWithCancel(long nativePtr) throws VException;
    private native CancelableVContext nativeWithDeadline(long nativePtr, DateTime deadline)
        throws VException;
    private native CancelableVContext nativeWithTimeout(long nativePtr, Duration timeout)
        throws VException;
    private native VContext nativeWithValue(long nativePtr, Object key, Object value)
        throws VException;
    private native void nativeCancel(long nativeCancelPtr) throws VException;
    private native void nativeFinalize(long nativePtr, long nativeCancelPtr);

    private VContextImpl(long nativePtr, long nativeCancelPtr) {
        this.nativePtr = nativePtr;
        this.nativeCancelPtr = nativeCancelPtr;
    }
    @Override
    public DateTime deadline() {
        try {
            return nativeDeadline(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get deadline", e);
        }
    }
    @Override
    public CountDownLatch done() {
        // NOTE(spetrovic): We may have to lock needlessly if nativeDone() returns a null
        // CountDownLatch, but that's OK for now.
        if (this.doneLatch != null) return this.doneLatch;
        synchronized (this) {
            if (this.doneLatch != null) return this.doneLatch;
            try {
                this.doneLatch = nativeDone(this.nativePtr);
                return this.doneLatch;
            } catch (VException e) {
                throw new RuntimeException("Couldn't invoke done", e);
            }
        }
    }
    @Override
    public Object value(Object key) {
        try {
            return nativeValue(this.nativePtr, key);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get value: ", e);
        }
    }
    @Override
    public CancelableVContext withCancel() {
        try {
            return nativeWithCancel(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't create cancelable context", e);
        }
    }
    @Override
    public CancelableVContext withDeadline(DateTime deadline) {
        try {
            return nativeWithDeadline(this.nativePtr, deadline);
        } catch (VException e) {
            throw new RuntimeException("Couldn't create context with deadline", e);
        }
    }
    @Override
    public CancelableVContext withTimeout(Duration timeout) {
        try {
            return nativeWithTimeout(this.nativePtr, timeout);
        } catch (VException e) {
            throw new RuntimeException("Couldn't create context with timeout", e);
        }
    }
    @Override
    public io.v.v23.context.VContext withValue(Object key, Object value) {
        try {
            return nativeWithValue(this.nativePtr, key, value);
        } catch (VException e) {
            throw new RuntimeException("Couldn't create context with data:", e);
        }
    }
    @Override
    public void cancel() {
        try {
            nativeCancel(this.nativeCancelPtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't cancel context", e);
        }
    }
    @Override
    void implementationsOnlyInThisPackage() {}
    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr, this.nativeCancelPtr);
    }
    private long nativePtr() {
        return this.nativePtr;
    }
}