// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.rpc;

import com.google.common.util.concurrent.ListenableFuture;

import io.v.impl.google.ListenableFutureCallback;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Callback;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerStatus;
import io.v.v23.verror.VException;

public class ServerImpl implements Server {
    private final long nativeRef;

    private native void nativeAddName(long nativeRef, String name) throws VException;
    private native void nativeRemoveName(long nativeRef, String name);
    private native ServerStatus nativeGetStatus(long nativeRef) throws VException;
    private native void nativeAllPublished(long nativeRef, VContext ctx, Callback<Void> callback);
    private native void nativeFinalize(long nativeRef);

    private ServerImpl(long nativeRef) {
        this.nativeRef = nativeRef;
    }
    // Implement io.v.v23.rpc.Server.
    @Override
    public void addName(String name) throws VException {
        nativeAddName(this.nativeRef, name);
    }
    @Override
    public void removeName(String name) {
        nativeRemoveName(this.nativeRef, name);
    }
    @Override
    public ServerStatus getStatus() {
        try {
            return nativeGetStatus(this.nativeRef);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get status", e);
        }
    }
    @Override
    public ListenableFuture<Void> allPublished(VContext ctx) {
        ListenableFutureCallback<Void> callback = new ListenableFutureCallback<>();
        nativeAllPublished(nativeRef, ctx, callback);
        return callback.getFuture(ctx);
    }
    // Implement java.lang.Object.
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (this.getClass() != other.getClass()) return false;
        return this.nativeRef == ((ServerImpl) other).nativeRef;
    }
    @Override
    public int hashCode() {
        return Long.valueOf(this.nativeRef).hashCode();
    }
    @Override
    protected void finalize() {
        nativeFinalize(this.nativeRef);
    }
}
