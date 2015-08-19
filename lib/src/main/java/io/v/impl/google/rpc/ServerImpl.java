// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.rpc;

import io.v.impl.google.channel.ChannelIterable;
import io.v.v23.naming.Endpoint;
import io.v.v23.rpc.Dispatcher;
import io.v.v23.rpc.Invoker;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.NetworkChange;
import io.v.v23.rpc.ReflectInvoker;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerStatus;
import io.v.v23.rpc.ServiceObjectWithAuthorizer;
import io.v.v23.security.Authorizer;
import io.v.v23.verror.VException;

public class ServerImpl implements Server {
    private final long nativePtr;

    private native Endpoint[] nativeListen(long nativePtr, ListenSpec spec) throws VException;
    private native void nativeServe(long nativePtr, String name, Dispatcher dispatcher)
        throws VException;
    private native void nativeAddName(long nativePtr, String name) throws VException;
    private native void nativeRemoveName(long nativePtr, String name);
    private native ServerStatus nativeGetStatus(long nativePtr) throws VException;
    private native Iterable<NetworkChange> nativeWatchNetwork(long nativePtr) throws VException;
    private native void nativeUnwatchNetwork(long nativePtr, ChannelIterable<NetworkChange> channel)
            throws VException;
    private native void nativeStop(long nativePtr) throws VException;
    private native void nativeFinalize(long nativePtr);

    private ServerImpl(long nativePtr) {
        this.nativePtr = nativePtr;
    }
    // Implement io.v.v23.rpc.Server.
    @Override
    public Endpoint[] listen(ListenSpec spec) throws VException {
        return nativeListen(this.nativePtr, spec);
    }
    @Override
    public void serve(String name, Object object, Authorizer auth) throws VException {
        if (object == null) {
            throw new VException("Serve called with a null object");
        }
        Invoker invoker = object instanceof Invoker ? (Invoker) object : new ReflectInvoker(object);
        nativeServe(this.nativePtr, name, new DefaultDispatcher(invoker, auth));
    }
    @Override
    public void serveDispatcher(String name, Dispatcher disp) throws VException {
        nativeServe(this.nativePtr, name, disp);
    }
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
    public Iterable<NetworkChange> watchNetwork() {
        try {
            return nativeWatchNetwork(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't watch network", e);
        }
    }
    @Override
    public void unwatchNetwork(Iterable<NetworkChange> it) {
        if (it == null || !(it instanceof ChannelIterable)) {
            return;
        }
        try {
            nativeUnwatchNetwork(this.nativePtr, (ChannelIterable<NetworkChange>) it);
        } catch (VException e) {
            throw new RuntimeException("Couldn't unwatch network", e);
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

    private static class DefaultDispatcher implements Dispatcher {
        private final Invoker invoker;
        private final Authorizer auth;

        DefaultDispatcher(Invoker invoker, Authorizer auth) {
            this.invoker = invoker;
            this.auth = auth;
        }
        @Override
        public ServiceObjectWithAuthorizer lookup(String suffix) throws VException {
            return new ServiceObjectWithAuthorizer(this.invoker, this.auth);
        }
    }
}