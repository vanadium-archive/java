// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23;

import com.google.common.io.ByteStreams;
import com.google.common.io.Resources;
import io.v.v23.context.VContext;
import io.v.v23.namespace.Namespace;
import io.v.v23.rpc.Client;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.Server;
import io.v.v23.security.CaveatRegistry;
import io.v.v23.security.ConstCaveatValidator;
import io.v.v23.security.ExpiryCaveatValidator;
import io.v.v23.security.MethodCaveatValidator;
import io.v.v23.security.Principal;
import io.v.v23.verror.VException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Class {@code V} represents the local environment allowing clients and servers to communicate
 * with one another.  The expected usage pattern of this class goes something like this:
 * <code>
 *    ...
 *    final VContext ctx = V.init(opts);
 *    ...
 *    final Server s = V.newServer(ctx);
 *    ...
 *    final Client c = V.getClient(ctx);
 *    ...
 * </code>
 */
public class V {
    private static native void nativeInit();

    private static volatile VContext context = null;
    private static volatile VRuntime runtime = null;

    private static void loadV23Library() {
        // First, attempt to find the library in java.library.path.
        List<Throwable> errors = new ArrayList<>();
        try {
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
            } catch (IOException | UnsatisfiedLinkError e) {
                errors.add(new RuntimeException("error while reading libv23.so from the classpath", e));
                throw new RuntimeException("Could not load v23 native library", new VLoaderException(errors));
            }
        }
        nativeInit();
    }
    /**
     * Initializes the Veyron environment, returning the base context.  Calling this method multiple
     * times will always return the result of the first call to {@code init()}, ignoring
     * subsequently provided options.
     *
     * This method loads the native Vanadium implementation if it has not already been loaded. It
     * method searches for the native Vanadium library using {@link System#loadLibrary}. If that
     * throws, then the method will look for the library in the root of the classpath. If it is
     * found, the bytes of the library are extracted to a temporary file and loaded with
     * {@link System#load}.
     *
     * If the above procedure fails to load the native implementation, a {@link RuntimeException}
     * will be thrown. The {@link RuntimeException#getCause cause} of the exception will be a
     * {@link VLoaderException} indicating the exceptions that occurred while attempting to load
     * the library.
     *
     * A caller may pass the following option that specifies the runtime implementation to be used:
     *     {@code OptionDefs.RUNTIME}
     *
     * If this option isn't provided, the default runtime implementation is used.
     *
     * @param  opts options
     * @return      base context
     */
    public static VContext init(Options opts) {
        if (context != null) return context;
        synchronized (V.class) {
            if (context != null) return context;
            loadV23Library();
            if (opts == null) opts = new Options();
            // See if a runtime was provided as an option.
            if (opts.get(OptionDefs.RUNTIME) != null) {
                runtime = opts.get(OptionDefs.RUNTIME, VRuntime.class);
            } else {
                // Use the default runtime implementation.
                try {
                    runtime = io.v.impl.google.rt.VRuntime.create(opts);
                } catch (VException e) {
                    throw new RuntimeException("Couldn't initialize Google Veyron Runtime", e);
                }
            }
            // Register caveat validators.
            try {
                CaveatRegistry.register(
                        io.v.v23.security.Constants.CONST_CAVEAT,
                        new ConstCaveatValidator());
                CaveatRegistry.register(
                        io.v.v23.security.Constants.EXPIRY_CAVEAT,
                        new ExpiryCaveatValidator());
                CaveatRegistry.register(io.v.v23.security.Constants.METHOD_CAVEAT,
                        new MethodCaveatValidator());
            } catch (VException e) {
                throw new RuntimeException("Couldn't register caveat validators", e);
            }
            context = runtime.getContext();

            // Set the VException component name to this binary name.
            context = VException.contextWithComponentName(
                    context, System.getProperty("program.name", ""));
            return context;
        }
    }

    /**
     * Initializes the Veyron environment without options.  See {@code init(Options)} for more
     * information.
     *
     * @return base context
     */
    public static VContext init() {
        return V.init(null);
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
     *     CURRENTLY NO OPTIONS ARE MANDATED
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
     * Creates a new server instance.
     *
     * @param  ctx             current context
     * @return                 the new server instance
     * @throws VException      if a new server cannot be created
     */
    public static Server newServer(VContext ctx) throws VException {
        return newServer(ctx, null);
    }

    /**
     * Creates a new server instance with the provided options.  A particular runtime
     * implementation chooses which options to support, but at the minimum it must handle
     * the following options:
     *     CURRENTLY NO OPTIONS ARE MANDATED
     *
     * @param  ctx             current context
     * @param  opts            server options
     * @return                 the new server instance
     * @throws VException      if a new server cannot be created
     */
    public static Server newServer(VContext ctx, Options opts) throws VException {
        if (opts == null) opts = new Options();
        return getRuntime().newServer(ctx, opts);
    }


    /**
     * Attaches the given principal to a new context (that is derived from the given context).
     *
     * @param  ctx             current context
     * @param  principal       principal to be attached
     * @return                 child context to which the principal is attached
     * @throws VException      if the principal couldn't be attached
     */
    public static VContext setPrincipal(VContext ctx, Principal principal) throws VException {
        return getRuntime().setPrincipal(ctx, principal);
    }

    /**
     * Returns the principal attached to the given context.
     *
     * @param  ctx current context
     * @return     the principal attached to the given context
     */
    public static Principal getPrincipal(VContext ctx) {
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

    private static VRuntime getRuntime() {
        init(null);
        return runtime;
    }
}
