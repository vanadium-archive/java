// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.rt;

import org.joda.time.Duration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.v.v23.OptionDefs;
import io.v.v23.Options;
import io.v.v23.VRuntime;
import io.v.v23.context.VContext;
import io.v.v23.discovery.VDiscovery;
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
    private static native VContext nativeInit() throws VException;
    private static native void nativeShutdown(VContext context);
    private static native VContext nativeWithNewClient(VContext ctx, Options opts)
            throws VException;
    private static native Client nativeGetClient(VContext ctx) throws VException;
    private static native VContext nativeWithNewServer(VContext ctx, String name,
                                                       Dispatcher dispatcher,
                                                       Executor executor,
                                                       Duration lameDuckTimeout) throws VException;
    private static native VContext nativeWithPrincipal(VContext ctx, VPrincipal principal)
            throws VException;
    private static native VPrincipal nativeGetPrincipal(VContext ctx) throws VException;
    private static native VContext nativeWithNewNamespace(VContext ctx, String... roots)
            throws VException;
    private static native Namespace nativeGetNamespace(VContext ctx) throws VException;
    private static native VContext nativeWithListenSpec(VContext ctx, ListenSpec spec)
            throws VException;
    private static native ListenSpec nativeGetListenSpec(VContext ctx) throws VException;

    private static native VDiscovery nativeGetDiscovery(VContext ctx) throws VException;

    // Attaches a server to the given context.  Used by this class and other classes
    // that natively create a server.
    private static VContext withServer(VContext ctx, Server server) {
        return ctx.withValue(new ServerKey(), server);
    }

    /**
     * Returns a new runtime instance.
     */
    public static VRuntimeImpl create(Options opts) throws VException {
        Executor executor = executorFromOptions(opts);
        if (executor == null) {
            executor = Executors.newCachedThreadPool();
        }
        return new VRuntimeImpl(nativeInit(), executor);
    }

    private final VContext ctx;  // non-null
    private final Executor executor;  // non-null

    private VRuntimeImpl(VContext ctx, Executor executor) {
        this.ctx = ctx;
        this.executor = executor;
    }
    @Override
    public VContext withNewClient(VContext ctx, Options opts) throws VException {
        return nativeWithNewClient(ctx, opts);
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
    public VContext withNewServer(VContext ctx, String name, Dispatcher disp, Options opts)
            throws VException {
        Executor executor = executorFromOptions(opts);
        if (executor == null) {
            executor = this.executor;
        }
        return nativeWithNewServer(ctx, name, disp, executor, lameDuckTimeoutFromOptions(opts));
    }
    @Override
    public VContext withNewServer(VContext ctx, String name, Object object, Authorizer authorizer,
                                  Options opts) throws VException {
        if (object == null) {
            throw new VException("newServer called with a null object");
        }
        Invoker invoker = object instanceof Invoker ? (Invoker) object : new ReflectInvoker(object);
        return withNewServer(ctx, name, new DefaultDispatcher(invoker, authorizer), opts);
    }
    @Override
    public Server getServer(VContext ctx) {
        Server server = (Server) ctx.value(new ServerKey());
        return server;
    }
    @Override
    public VContext withPrincipal(VContext ctx, VPrincipal principal) throws VException {
        return nativeWithPrincipal(ctx, principal);
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
    public VContext withNewNamespace(VContext ctx, String... roots) throws VException {
        return nativeWithNewNamespace(ctx, roots);
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
    public VContext withListenSpec(VContext ctx, ListenSpec spec) throws VException {
        return nativeWithListenSpec(ctx, spec);
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
    public VDiscovery getDiscovery(VContext ctx) {
        try {
            return nativeGetDiscovery(ctx);
        } catch (VException e) {
            throw new RuntimeException("Couldn't get discovery: ", e);
        }
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

    private static class ServerKey {
        @Override
        public int hashCode() {
            return 0;
        }
    }

    private static Executor executorFromOptions(Options opts) {
        if (!opts.has(OptionDefs.SERVER_THREAD_EXECUTOR)) {
            return null;
        }
        Object executorOpt = opts.get(OptionDefs.SERVER_THREAD_EXECUTOR);
        if (!(executorOpt instanceof Executor)) {
            throw new RuntimeException("SERVER_THREAD_EXECUTOR option, if specified, must " +
                    "contain an object of type java.util.concurrent.Executor.");
        }
        return (Executor) executorOpt;
    }

    private static Duration lameDuckTimeoutFromOptions(Options opts) {
        if (!opts.has(OptionDefs.SERVER_LAME_DUCK_TIMEOUT)) {
            return Duration.standardSeconds(5);
        }

        Object timeout = opts.get(OptionDefs.SERVER_LAME_DUCK_TIMEOUT);

        if (!(timeout instanceof Duration)) {
            throw new RuntimeException("SERVER_LAME_DUCK_TIMEOUT option if specified must " +
                        "contain an object of type org.joda.time.Duration");
        }
        return (Duration) timeout;
    }
}
