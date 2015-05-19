// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.security;

import org.joda.time.DateTime;

import java.util.Map;

import io.v.v23.vdl.VdlValue;
import io.v.v23.verror.VException;

class CallImpl implements Call {
    private final long nativePtr;

    private native DateTime nativeTimestamp(long nativePtr) throws VException;
    private native String nativeMethod(long nativePtr);
    private native VdlValue[] nativeMethodTags(long nativePtr) throws VException;
    private native String nativeSuffix(long nativePtr);
    private native Map<String, Discharge> nativeLocalDischarges(long nativePtr);
    private native Map<String, Discharge> nativeRemoteDischarges(long nativePtr);
    private native String nativeLocalEndpoint(long nativePtr);
    private native String nativeRemoteEndpoint(long nativePtr);
    private native VPrincipal nativeLocalPrincipal(long nativePtr) throws VException;
    private native Blessings nativeLocalBlessings(long nativePtr) throws VException;
    private native Blessings nativeRemoteBlessings(long nativePtr) throws VException;
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
            throw new RuntimeException("Couldn't get timestamp", e);
        }
    }
    @Override
    public String method() {
        return nativeMethod(this.nativePtr);
    }
    @Override
    public VdlValue[] methodTags() {
        try {
            VdlValue[] tags = nativeMethodTags(this.nativePtr);
            return tags != null ? tags : new VdlValue[0];
        } catch (VException e) {
            throw new RuntimeException("Couldn't get method tags", e);
        }
    }
    @Override
    public String suffix() {
        return nativeSuffix(this.nativePtr);
    }
    @Override
    public Map<String, Discharge> localDischarges() {
        return nativeLocalDischarges(nativePtr);
    }
    @Override
    public Map<String, Discharge> remoteDischarges() {
        return nativeRemoteDischarges(nativePtr);
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
    public VPrincipal localPrincipal() {
        try {
            return nativeLocalPrincipal(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get local principal", e);
        }
    }
    @Override
    public Blessings localBlessings() {
        try {
            return nativeLocalBlessings(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get local blessings", e);
        }
    }
    @Override
    public Blessings remoteBlessings() {
        try {
            return nativeRemoteBlessings(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get remote blessings", e);
        }
    }
    // Implements java.lang.Object.
    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr);
    }
}