// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.security;

import org.joda.time.DateTime;

import io.v.v23.vdl.VdlValue;
import io.v.v23.verror.VException;

class CallImpl implements Call {
    private final long nativePtr;

    public native DateTime nativeTimestamp(long nativePtr) throws VException;
    public native String nativeMethod(long nativePtr);
    public native VdlValue[] nativeMethodTags(long nativePtr) throws VException;
    private native String nativeSuffix(long nativePtr);
    private native String nativeLocalEndpoint(long nativePtr);
    private native String nativeRemoteEndpoint(long nativePtr);
    private native Principal nativeLocalPrincipal(long nativePtr) throws VException;
    private native Blessings nativeLocalBlessings(long nativePtr) throws VException;
    private native Blessings nativeRemoteBlessings(long nativePtr) throws VException;
    private native io.v.v23.context.VContext nativeContext(long nativePtr) throws VException;
    private native void nativeFinalize(long nativePtr);

    CallImpl(long nativePtr) {
        this.nativePtr = nativePtr;
    }
    // Implements io.v.v23.security.VContext.
    @Override
    public DateTime timestamp() {
        try {
            return nativeTimestamp(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get timestamp: " + e.getMessage());
        }
    }
    @Override
    public String method() {
        return nativeMethod(this.nativePtr);
    }
    @Override
    public VdlValue[] methodTags() {
        try {
            final VdlValue[] tags = nativeMethodTags(this.nativePtr);
            return tags != null ? tags : new VdlValue[0];
        } catch (VException e) {
            throw new RuntimeException("Couldn't get method tags: " + e.getMessage());
        }
    }
    @Override
    public String suffix() {
        return nativeSuffix(this.nativePtr);
    }
    @Override
    public String localEndpoint() {
        return nativeLocalEndpoint(this.nativePtr);
    }
    @Override
    public String remoteEndpoint() {
        return nativeRemoteEndpoint(this.nativePtr);
    }
    @Override
    public Principal localPrincipal() {
        try {
            return nativeLocalPrincipal(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get local Principal: " + e.getMessage());
        }
    }
    @Override
    public Blessings localBlessings() {
        try {
            return nativeLocalBlessings(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get local Blessings: " + e.getMessage());
        }
    }
    @Override
    public Blessings remoteBlessings() {
        try {
            return nativeRemoteBlessings(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get remote Blessings: " + e.getMessage());
        }
    }
    @Override
    public io.v.v23.context.VContext context() {
        try {
            return nativeContext(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get Vanadium context: " + e.getMessage());
        }
    }
    // Implements java.lang.Object.
    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr);
    }
}