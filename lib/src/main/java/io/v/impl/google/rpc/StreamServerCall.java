// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.rpc;

import org.joda.time.DateTime;

import io.v.v23.rpc.Server;
import io.v.v23.security.Blessings;
import io.v.v23.security.Principal;
import io.v.v23.vdl.VdlValue;
import io.v.v23.verror.VException;

import java.io.EOFException;
import java.lang.reflect.Type;

public class StreamServerCall implements io.v.v23.rpc.StreamServerCall {
    private final long nativePtr;
    private final Stream stream;
    private final ServerCall serverCall;

    private native void nativeFinalize(long nativePtr);

    private StreamServerCall(long nativePtr, Stream stream, ServerCall serverCall) {
        this.nativePtr = nativePtr;
        this.stream = stream;
        this.serverCall = serverCall;
    }
    // Implements io.v.v23.ipc.Stream.
    @Override
    public void send(Object item, Type type) throws VException {
        this.stream.send(item, type);
    }
    @Override
    public Object recv(Type type) throws EOFException, VException {
        return this.stream.recv(type);
    }
    // Implements io.v.v23.ipc.ServerCall.
    @Override
    public Blessings grantedBlessings() {
        return serverCall.grantedBlessings();
    }

    @Override
    public Server server() {
        return serverCall.server();
    }

    // Implements io.v.v23.rpc.ServerCall.
    @Override
    public String suffix() {
        return this.serverCall.suffix();
    }
    @Override
    public String localEndpoint() {
        return this.serverCall.localEndpoint();
    }
    @Override
    public String remoteEndpoint() {
        return this.serverCall.remoteEndpoint();
    }
    @Override
    public io.v.v23.context.VContext context() {
        return this.serverCall.context();
    }
    // Implements java.lang.Object.
    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr);
    }
}