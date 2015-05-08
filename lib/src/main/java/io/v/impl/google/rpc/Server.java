// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.rpc;

import io.v.impl.google.channel.InputChannel;
import io.v.v23.rpc.Dispatcher;
import io.v.v23.rpc.Invoker;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.NetworkChange;
import io.v.v23.rpc.ReflectInvoker;
import io.v.v23.rpc.ServerStatus;
import io.v.v23.rpc.ServiceObjectWithAuthorizer;
import io.v.v23.verror.VException;

public class Server implements io.v.v23.rpc.Server {
    private final long nativePtr;

    private native String[] nativeListen(long nativePtr, ListenSpec spec) throws VException;
    private native void nativeServe(long nativePtr, String name, Dispatcher dispatcher)
        throws VException;
    private native void nativeAddName(long nativePtr, String name) throws VException;
    private native void nativeRemoveName(long nativePtr, String name);
    private native ServerStatus nativeGetStatus(long nativePtr) throws VException;
    private native InputChannel<NetworkChange> nativeWatchNetwork(long nativePtr) throws VException;
    private native void nativeUnwatchNetwork(long nativePtr, InputChannel<NetworkChange> channel)
            throws VException;
    private native void nativeStop(long nativePtr) throws VException;
    private native void nativeFinalize(long nativePtr);

    private Server(long nativePtr) {
        this.nativePtr = nativePtr;
    }
    // Implement io.v.v23.ipc.Server.
    @Override
    public String[] listen(ListenSpec spec) throws VException {
        return nativeListen(this.nativePtr, spec);
    }
    @Override
    public void serve(String name, Object object) throws VException {
        if (object == null) {
            throw new VException("Serve called with a null object");
        }
        if (object instanceof Dispatcher) {
            nativeServe(this.nativePtr, name, (Dispatcher)object);
            return;
        }
        Invoker invoker = object instanceof Invoker ? (Invoker) object : new ReflectInvoker(object);
        nativeServe(this.nativePtr, name, new DefaultDispatcher(invoker));
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
    public io.v.v23.InputChannel<NetworkChange> watchNetwork() {
        try {
            return nativeWatchNetwork(this.nativePtr);
        } catch (VException e) {
            throw new RuntimeException("Couldn't watch network", e);
        }
    }
    @Override
    public void unwatchNetwork(io.v.v23.InputChannel<NetworkChange> channel) {
        if (!(channel instanceof InputChannel)) {  // also handles channel == null
            return;
        }
        try {
            nativeUnwatchNetwork(this.nativePtr, (InputChannel<NetworkChange>) channel);
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
        return this.nativePtr == ((Server) other).nativePtr;
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

        DefaultDispatcher(Invoker invoker) {
            this.invoker = invoker;
        }
        @Override
        public ServiceObjectWithAuthorizer lookup(String suffix) throws VException {
            return new ServiceObjectWithAuthorizer(this.invoker, null);
        }
    }
}