// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.rpc;

import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerStatus;
import io.v.v23.verror.VException;

public class ServerImpl implements Server {
    private final long nativePtr;

    private native void nativeAddName(long nativePtr, String name) throws VException;
    private native void nativeRemoveName(long nativePtr, String name);
    private native ServerStatus nativeGetStatus(long nativePtr) throws VException;
    private native void nativeStop(long nativePtr) throws VException;
    private native void nativeFinalize(long nativePtr);

    private ServerImpl(long nativePtr) {
        this.nativePtr = nativePtr;
    }
    // Implement io.v.v23.rpc.Server.
    @Override
    public void addName(String name) throws VException {
        nativeAddName(this.nativePtr, name);
    }
    @Override
    public void removeName(String name) {
        nativeRemoveName(this.nativePtr, name);
    }
    @Override
    public ServerStatus getStatus() {
        try {
            return nativeGetStatus(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get status", e);
        }
    }
    @Override
    public void stop() throws VException {
        nativeStop(this.nativePtr);
    }
    // Implement java.lang.Object.
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (this.getClass() != other.getClass()) return false;
        return this.nativePtr == ((ServerImpl) other).nativePtr;
    }
    @Override
    public int hashCode() {
        return Long.valueOf(this.nativePtr).hashCode();
    }
    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr);
    }
}
