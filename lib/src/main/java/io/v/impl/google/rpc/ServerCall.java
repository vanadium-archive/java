// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.rpc;

import io.v.v23.context.VContext;
import io.v.v23.rpc.*;
import io.v.v23.rpc.Server;
import io.v.v23.security.Blessings;

public class ServerCall implements io.v.v23.rpc.ServerCall {
    private final long nativePtr;

    private static native String nativeSuffix(long nativePtr);
    private static native String nativeLocalEndpoint(long nativePtr);
    private static native String nativeRemoteEndpoint(long nativePtr);
    private static native Blessings nativeGrantedBlessings(long nativePtr);
    private static native Server nativeServer(long nativePtr);
    private static native void nativeFinalize(long nativePtr);

    private ServerCall(long nativePtr) {
        this.nativePtr = nativePtr;
    }

    @Override
    public String suffix() {
        return nativeSuffix(nativePtr);
    }

    @Override
    public String localEndpoint() {
        return nativeLocalEndpoint(nativePtr);
    }

    @Override
    public String remoteEndpoint() {
        return nativeRemoteEndpoint(nativePtr);
    }

    @Override
    public Blessings grantedBlessings() {
        return nativeGrantedBlessings(nativePtr);
    }

    @Override
    public Server server() {
        return nativeServer(nativePtr);
    }

    @Override
    protected void finalize() throws Throwable {
        nativeFinalize(nativePtr);
    }
}
