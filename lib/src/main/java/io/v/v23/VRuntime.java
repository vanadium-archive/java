// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23;

import io.v.v23.context.VContext;
import io.v.v23.namespace.Namespace;
import io.v.v23.rpc.Client;
import io.v.v23.rpc.Dispatcher;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.Server;
import io.v.v23.security.Authorizer;
import io.v.v23.security.VPrincipal;
import io.v.v23.verror.VException;

/**
 * The base interface for all runtime implementations.
 *
 * Methods in this class aren't meant to be invoked directly; instead, {@link V}
 * should be initialized with an instance of this class, passed in through the
 * {@link OptionDefs#RUNTIME} option.
 */
public interface VRuntime {
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
    VContext setNewClient(VContext ctx, Options opts) throws VException;

    /**
     * Returns the client attached to the given context.
     *
     * @param  ctx current context
     * @return     the client attached to the given context
     */
    Client getClient(VContext ctx);

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
    Server newServer(VContext ctx, String name, Object object, Authorizer auth, Options opts) throws VException;

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
    Server newServer(VContext ctx, String name, Dispatcher disp, Options opts) throws VException;

    /**
     * Attaches the given principal to a new context (that is derived from the given context).
     *
     * @param  ctx             current context
     * @param  principal       principal to be attached
     * @return                 child context to which the principal is attached
     * @throws VException      if the principal couldn't be attached
     */
    VContext setPrincipal(VContext ctx, VPrincipal principal) throws VException;

    /**
     * Returns the principal attached to the given context.
     *
     * @param  ctx current context
     * @return     the principal attached to the given context
     */
    VPrincipal getPrincipal(VContext ctx);

    /**
     * Creates a new namespace instance and attaches it to a new context.
     *
     * @param  ctx             current context
     * @param  roots           roots of the new namespace
     * @return                 child context to which the principal is attached
     * @throws VException      if the namespace couldn't be created
     */
    VContext setNewNamespace(VContext ctx, String... roots) throws VException;

    /**
     * Returns the namespace attached to the given context.
     *
     * @param  ctx current context
     * @return     the namespace attached to the given context.
     */
    Namespace getNamespace(VContext ctx);

    /**
     * Returns the {@code ListenSpec} attached to the given context.
     *
     * @param  ctx current context
     * @return     the {@code ListenSpec} attached to the given context
     */
    ListenSpec getListenSpec(VContext ctx);

    /**
     * Attaches the given {@code ListenSpec} to a new context.
     *
     * @param ctx        current context
     * @param spec       the {@code ListenSpec} to attach
     * @return           child context to which the {@code ListenSpec} is attached.
     */
    VContext setListenSpec(VContext ctx, ListenSpec spec) throws VException;

    /**
     * Returns the base context associated with the runtime.
     *
     * @return the base context associated with the runtime.
     */
    VContext getContext();

    /**
     * Shuts down the runtime, allowing the runtime to release resources, shutdown services and
     * the like.
     */
    void shutdown();
}
