// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import io.v.impl.google.rt.VRuntimeImpl;
import io.v.v23.context.VContext;
import io.v.v23.namespace.Namespace;
import io.v.v23.rpc.Client;
import io.v.v23.rpc.Dispatcher;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.Server;
import io.v.v23.security.Authorizer;
import io.v.v23.security.CaveatRegistry;
import io.v.v23.security.ConstCaveatValidator;
import io.v.v23.security.Constants;
import io.v.v23.security.ExpiryCaveatValidator;
import io.v.v23.security.MethodCaveatValidator;
import io.v.v23.security.PublicKeyThirdPartyCaveatValidator;
import io.v.v23.security.VPrincipal;
import io.v.v23.verror.VException;

/**
 * The local environment allowing clients and servers to communicate with one another.  The expected
 * usage pattern of this class goes something like this:
 * <p><blockquote><pre>
 *     VContext ctx = V.init(opts);
 *     ...
 *     Server s = V.newServer(ctx);
 *     ...
 *     Client c = V.getClient(ctx);
 *     ...
 * </pre></blockquote><p>
 */
public class V {
    private static native void nativeInit();
    private static native void nativeShutdown(VContext context);

    private static volatile VContext context = null;
    private static volatile VRuntime runtime = null;
    private static volatile boolean initOnceDone = false;

    private static synchronized void initOnce() {
        if (initOnceDone) {
            return;
        }
        List<Throwable> errors = new ArrayList<Throwable>();
        try {
            // First, attempt to find the library in java.library.path.
            System.loadLibrary("v23");
        } catch (UnsatisfiedLinkError ule) {
            // Thrown if the library does not exist. In this case, try to find it in our classpath.
            errors.add(new RuntimeException("loadLibrary attempt failed", ule));
            try {
                URL resource = Resources.getResource("libv23.so");
                File file = File.createTempFile("libv23-", ".so");
                file.deleteOnExit();
                ByteStreams.copy(resource.openStream(), new FileOutputStream(file));
                System.load(file.getAbsolutePath());
            } catch (IllegalArgumentException iae) {
                errors.add(new RuntimeException("couldn't locate libv23.so on the classpath", iae));
                throw new RuntimeException("Could not load v23 native library", new VLoaderException(errors));
            } catch (IOException e) {
                errors.add(new RuntimeException("error while reading libv23.so from the classpath", e));
                throw new RuntimeException("Could not load v23 native library", new VLoaderException(errors));
            } catch (UnsatisfiedLinkError e) {
                errors.add(new RuntimeException("error while reading libv23.so from the classpath", e));
                throw new RuntimeException("Could not load v23 native library", new VLoaderException(errors));
            }
        }
        nativeInit();

        // Register caveat validators.
        try {
            CaveatRegistry.register(
                    io.v.v23.security.Constants.CONST_CAVEAT,
                    ConstCaveatValidator.INSTANCE);
            CaveatRegistry.register(
                    io.v.v23.security.Constants.EXPIRY_CAVEAT,
                    ExpiryCaveatValidator.INSTANCE);
            CaveatRegistry.register(io.v.v23.security.Constants.METHOD_CAVEAT,
                    MethodCaveatValidator.INSTANCE);
            CaveatRegistry.register(Constants.PUBLIC_KEY_THIRD_PARTY_CAVEAT,
                    PublicKeyThirdPartyCaveatValidator.INSTANCE);
        } catch (VException e) {
            throw new RuntimeException("Couldn't register caveat validators", e);
        }

        initOnceDone = true;
    }
    /**
     * Initializes the Vanadium environment, returning the base context.  Calling this method
     * multiple times will always return the result of the first call to {@link #init init},
     * ignoring subsequently provided options, unless you first call {@link #shutdown}.
     * <p>
     * This method loads the native Vanadium implementation if it has not already been loaded. It
     * searches for the native Vanadium library using {@link java.lang.System#loadLibrary}.
     * If that throws, then the method will look for the library in the root of the classpath.
     * If it is found, the bytes of the library are extracted to a temporary file and loaded with
     * {@link java.lang.System#load}.
     * <p>
     * If the above procedure fails to load the native implementation, a {@link RuntimeException}
     * will be thrown. The {@link RuntimeException#getCause cause} of the exception will be a
     * {@link VLoaderException} indicating the exceptions that occurred while attempting to load
     * the library.
     * <p>
     * A caller may pass the following option that specifies the runtime implementation to be used:
     * <p><ul>
     *     <li>{@link OptionDefs#RUNTIME}</li>
     * </ul><p>
     * If this option isn't provided, the default runtime implementation is used.
     *
     * @param  opts options
     * @return      base context
     */
    public static VContext init(Options opts) {
        if (context != null) return context;
        synchronized (V.class) {
            if (context != null) return context;
            initOnce();
            if (opts == null) opts = new Options();
            // See if a runtime was provided as an option.
            if (opts.get(OptionDefs.RUNTIME) != null) {
                runtime = opts.get(OptionDefs.RUNTIME, VRuntime.class);
            } else {
                // Use the default runtime implementation.
                try {
                    runtime = VRuntimeImpl.create(opts);
                } catch (VException e) {
                    throw new RuntimeException("Couldn't initialize Google Vanadium Runtime", e);
                }
            }
            context = runtime.getContext();

            // Set the VException component name to this binary name.
            context = VException.contextWithComponentName(
                    context, System.getProperty("program.name", ""));
            return context;
        }
    }

    /**
     * Initializes the Vanadium environment without options.  See {@link #init(Options)} for more
     * information.
     *
     * @return base context
     */
    public static VContext init() {
        return V.init(null);
    }

    /**
     * Shuts down the Vanadium environment. It is an error to call this method before calling
     * {@link #init}, or more than once per call to {@link #init}.
     *
     * <p>After this call, you may initialize a new environment again by calling {@link #init}.
     */
    public static void shutdown() {
        synchronized (V.class) {
            Preconditions.checkState(context != null,
                    "no context to shutdown, did you call init()?");
            runtime.shutdown();
            context = null;
            runtime = null;
        }
    }

    /**
     * Creates a new client instance and attaches it to a new context.
     *
     * @param  ctx             current context
     * @return                 child context to which the new client is attached
     * @throws VException      if a new client cannot be created
     */
    public static VContext setNewClient(VContext ctx) throws VException {
        return setNewClient(ctx, null);
    }

    /**
     * Creates a new client instance with the provided options and attaches it to a new context.
     * A particular runtime implementation chooses which options to support, but at the minimum must
     * handle the following options:
     * <p><ul>
     *     <li>(CURRENTLY NO OPTIONS ARE MANDATED)</li>
     * </ul>
     *
     * @param  ctx             current context
     * @param  opts            client options
     * @return                 child context to which the new client is attached
     * @throws VException      if a new client cannot be created
     */
    public static VContext setNewClient(VContext ctx, Options opts) throws VException {
        if (opts == null) opts = new Options();
        return getRuntime().setNewClient(ctx, opts);
    }

    /**
     * Returns the client attached to the given context.
     *
     * @param  ctx current context
     * @return     the client attached to the given context
     */
    public static Client getClient(VContext ctx) {
        return getRuntime().getClient(ctx);
    }

    /**
     * Creates a new Server instance to serve a service object.
     *
     * The server will listen for network connections as specified by
     * the {@link ListenSpec} attached to the context. Depending on
     * your runtime, 'roaming' support may be enabled. In this mode
     * the server will adapt to changes in the network configuration
     * and re-publish the current set of endpoints to the mount table
     * accordingly.
     * <p>
     * This call associates object with name by publishing the address
     * of this server with the mount table under the supplied name and
     * using the given authorizer to authorize access to it.  RPCs
     * invoked on the supplied name will be delivered to methods
     * implemented by the supplied object.
     * <p> 
     * Reflection is used to match requests to the object's method
     * set.  As a special-case, if the object implements the 
     * {@link Invoker} interface, the invoker is used to invoke methods
     * directly, without reflection.
     * <p>
     * If name is an empty string, no attempt will made to publish
     * that name to a mount table.
     * <p>
     * If the passed-in authorizer is {@code null}, the default
     * authorizer will be used.  (The default authorizer uses the
     * blessing chain derivation to determine if the client is
     * authorized to access the object's methods.)
     *
     * @param  ctx             current context
     * @param  name            name under which the supplied object should be published,
     *                         or the empty string if the object should not be published
     * @param  object          object to be published under the given name
     * @param  auth            authorizer that will control access to objects methods
     * @return                 the new server instance
     * @throws VException      if a new server cannot be created
     */
    public static Server newServer(VContext ctx, String name, Object object, Authorizer authorizer) throws VException {
        return newServer(ctx, name, object, authorizer, null);
    }

    /**
     * Creates a new Server instance to serve a service object.
     *
     * Same as {@link #newServer(VContext,String,Object,Authorizer}
     * but accepts implementation-specific server-creation options.
     * <p>
     * A particular runtime implementation chooses which options to support,
     * but at the minimum it must handle the following options:
     * <p><ul>
     *     <li>(CURRENTLY NO OPTIONS ARE MANDATED)</li>
     * </ul>
     *
     * @param  ctx             current context
     * @param  name            name under which the supplied object should be published,
     *                         or the empty string if the object should not be published
     * @param  object          object to be published under the given name
     * @param  auth            authorizer that will control access to objects methods
     * @param  opts            server options
     * @return                 the new server instance
     * @throws VException      if a new server cannot be created
     */
    public static Server newServer(VContext ctx, String name, Object object, Authorizer authorizer, Options opts) throws VException {
        if (opts == null) {
            opts = new Options();
        }
        return getRuntime().newServer(ctx, name, object, authorizer, opts);
    }

    /**
     * Creates a new Server instance to serve a dispatcher.
     *
     * The server will listen for network connections as specified by
     * the {@link ListenSpec} attached to the context. Depending on
     * your runtime, 'roaming' support may be enabled. In this mode
     * the server will adapt to changes in the network configuration
     * and re-publish the current set of endpoints to the mount table
     * accordingly.
     * <p>
     * Associates dispatcher with the portion of the mount table's
     * name space for which {@code name} is a prefix, by publishing
     * the address of this dispatcher with the mount table under the
     * supplied name.
     * <p> 
     * RPCs invoked on the supplied name will be delivered to the
     * supplied {@link Dispatcher}'s {@link Dispatcher#lookup lookup}
     * method which will in turn return the object and
     * {@link Authorizer} used to serve the actual RPC call.
     * <p>
     * If name is an empty string, no attempt will made to publish
     * that name to a mount table.
     *
     * @param  ctx             current context
     * @param  name            name under which the supplied object should be published,
     *                         or the empty string if the object should not be published
     * @param  disp            dispatcher to be published under the given name
     * @return                 the new server instance
     * @throws VException      if a new server cannot be created
     */
    public static Server newServer(VContext ctx, String name, Dispatcher disp) throws VException {
        return newServer(ctx, name, disp, (Options)null);
    }

    /**
     * Creates a new Server instance to serve a dispatcher.
     *
     * Same as {@link #newServer(VContext,String,Dispatcher} but
     * accepts implementation-specific server-creation options.
     * <p>
     * A particular runtime implementation chooses which options to support,
     * but at the minimum it must handle the following options:
     * <p><ul>
     *     <li>(CURRENTLY NO OPTIONS ARE MANDATED)</li>
     * </ul>
     *
     * @param  ctx             current context
     * @param  name            name under which the supplied object should be published,
     *                         or the empty string if the object should not be published
     * @param  disp            dispatcher to be published under the given name
     * @param  opts            server options
     * @return                 the new server instance
     * @throws VException      if a new server cannot be created
     */
    public static Server newServer(VContext ctx, String name, Dispatcher disp, Options opts) throws VException {
        if (opts == null) {
            opts = new Options();
        }
        return getRuntime().newServer(ctx, name, disp, opts);
    }

    /**
     * Attaches the given principal to a new context (that is derived from the given context).
     *
     * @param  ctx             current context
     * @param  principal       principal to be attached
     * @return                 child context to which the principal is attached
     * @throws VException      if the principal couldn't be attached
     */
    public static VContext setPrincipal(VContext ctx, VPrincipal principal) throws VException {
        return getRuntime().setPrincipal(ctx, principal);
    }

    /**
     * Returns the principal attached to the given context.
     *
     * @param  ctx current context
     * @return     the principal attached to the given context
     */
    public static VPrincipal getPrincipal(VContext ctx) {
        return getRuntime().getPrincipal(ctx);
    }

    /**
     * Creates a new namespace instance and attaches it to a new context.
     *
     * @param  ctx             current context
     * @param  roots           roots of the namespace
     * @return                 child context to which the principal is attached
     * @throws VException      if the namespace couldn't be created
     */
    public static VContext setNewNamespace(VContext ctx, String... roots) throws VException {
        return getRuntime().setNewNamespace(ctx, roots);
    }

    /**
     * Returns the namespace attached to the given context.
     *
     * @param  ctx current context
     * @return     the namespace attached to the given context.
     */
    public static Namespace getNamespace(VContext ctx) {
        return getRuntime().getNamespace(ctx);
    }

    /**
     * Returns the {@code ListenSpec} attached to the given context.
     *
     * @param  ctx current context
     * @return     the {@code ListenSpec} attached to the given context
     */
    public static ListenSpec getListenSpec(VContext ctx) {
        return getRuntime().getListenSpec(ctx);
    }

    /**
     * Attaches the given {@code ListenSpec} to a new context.
     *
     * @param ctx        current context
     * @param spec       the {@code ListenSpec} to attach
     * @return           child context to which the {@code ListenSpec} is attached.
     */
    public static VContext setListenSpec(VContext ctx, ListenSpec spec) throws VException {
        return getRuntime().setListenSpec(ctx, spec);
    }

    private static VRuntime getRuntime() {
        init(null);
        return runtime;
    }

    protected V() {}
}
