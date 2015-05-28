// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23;

import io.v.v23.context.VContext;
import io.v.v23.namespace.Namespace;
import io.v.v23.rpc.Client;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.Server;
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
     * Creates a new server instance with the provided options.  A particular runtime
     * implementation chooses which options to support, but at the minimum it must handle
     * the following options:
     * <p><ul>
     *     <li>(CURRENTLY NO OPTIONS ARE MANDATED)</li>
     * </ul>
     *
     * @param  ctx             current context
     * @param  opts            server options
     * @return                 the new server instance
     * @throws VException      if a new server cannot be created
     */
    Server newServer(VContext ctx, Options opts) throws VException;

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