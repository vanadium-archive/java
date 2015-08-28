// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.rt;

import io.v.v23.OptionDefs;
import io.v.v23.Options;
import io.v.v23.VRuntime;
import io.v.v23.context.VContext;
import io.v.v23.namespace.Namespace;
import io.v.v23.rpc.Client;
import io.v.v23.rpc.Dispatcher;
import io.v.v23.rpc.Invoker;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.ReflectInvoker;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServiceObjectWithAuthorizer;
import io.v.v23.security.Authorizer;
import io.v.v23.security.VPrincipal;
import io.v.v23.verror.VException;

/**
 * An implementation of {@link io.v.v23.VRuntime} interface that calls to native
 * code for most of its functionalities.
 */
public class VRuntimeImpl implements VRuntime {
    private static native VContext nativeInit(int numCpus) throws VException;
    private static native void nativeShutdown(VContext context);
    private static native VContext nativeSetNewClient(VContext ctx, Options opts)
            throws VException;
    private static native Client nativeGetClient(VContext ctx)
            throws VException;
    private static native Server nativeNewServer(VContext ctx, String name, Dispatcher dispatcher) 
            throws VException;
    private static native VContext nativeSetPrincipal(VContext ctx, VPrincipal principal)
            throws VException;
    private static native VPrincipal nativeGetPrincipal(VContext ctx) throws VException;
    private static native VContext nativeSetNewNamespace(VContext ctx, String... roots)
            throws VException;
    private static native Namespace nativeGetNamespace(VContext ctx) throws VException;
    private static native ListenSpec nativeGetListenSpec(VContext ctx) throws VException;
    private static native VContext nativeSetListenSpec(VContext ctx, ListenSpec spec) throws VException;

    /**
     * Returns a new runtime instance.
     */
    public static VRuntimeImpl create(Options opts) throws VException {
        int numCpus = opts.has(OptionDefs.RUNTIME_NUM_CPUS)
                ? opts.get(OptionDefs.RUNTIME_NUM_CPUS, Integer.class)
                : 1;
        if (numCpus < 1) {
            numCpus = 1;
        }
        return new VRuntimeImpl(nativeInit(numCpus));
    }

    private final VContext ctx;  // non-null

    private VRuntimeImpl(VContext ctx) {
        this.ctx = ctx;
    }
    @Override
    public VContext setNewClient(VContext ctx, Options opts) throws VException {
        return nativeSetNewClient(ctx, opts);
    }
    @Override
    public Client getClient(VContext ctx) {
        try {
            return nativeGetClient(ctx);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get client", e);
        }
    }
    @Override
    public Server newServer(VContext ctx, String name, Dispatcher disp, Options opts) throws VException {
        return nativeNewServer(ctx, name, disp);
    }

    @Override
    public Server newServer(VContext ctx, String name, Object object, Authorizer authorizer, Options opts) throws VException {
        if (object == null) {
            throw new VException("newServer called with a null object");
        }
        Invoker invoker = object instanceof Invoker ? (Invoker) object : new ReflectInvoker(object);
        return nativeNewServer(ctx, name, new DefaultDispatcher(invoker, authorizer));
    }

    @Override
    public VContext setPrincipal(VContext ctx, VPrincipal principal) throws VException {
        return nativeSetPrincipal(ctx, principal);
    }
    @Override
    public VPrincipal getPrincipal(VContext ctx) {
        try {
            return nativeGetPrincipal(ctx);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get principal", e);
        }
    }
    @Override
    public VContext setNewNamespace(VContext ctx, String... roots) throws VException {
        return nativeSetNewNamespace(ctx, roots);
    }
    @Override
    public Namespace getNamespace(VContext ctx) {
        try {
            return nativeGetNamespace(ctx);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get namespace", e);
        }
    }
    @Override
    public ListenSpec getListenSpec(VContext ctx) {
        try {
            return nativeGetListenSpec(ctx);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get listen spec: ", e);
        }
    }
    @Override
    public VContext setListenSpec(VContext ctx, ListenSpec spec) throws VException {
        return nativeSetListenSpec(ctx, spec);
    }
    @Override
    public VContext getContext() {
        return this.ctx;
    }
    @Override
    public void shutdown() {
        nativeShutdown(ctx);
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
