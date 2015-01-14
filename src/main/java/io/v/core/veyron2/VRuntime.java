package io.v.core.veyron2;

import io.v.core.veyron2.context.VContext;
import io.v.core.veyron2.ipc.Client;
import io.v.core.veyron2.ipc.Server;
import io.v.core.veyron2.naming.Namespace;
import io.v.core.veyron2.security.Principal;

/**
 * VRuntime is a base interface for all runtime implementations.
 *
 * Methods in this class aren't meant to be invoked directly; instead, {@code V}
 * should be initialized with an instance of this class, passed in through the
 * {@code OptionDefs.RUNTIME} option.
 */
public interface VRuntime {
	/**
	 * Creates a new client instance with the provided options and attaches it to a new context.
	 * A particular runtime implementation chooses which options to support, but at the minimum must
	 * handle the following options:
	 *     CURRENTLY NO OPTIONS ARE MANDATED
	 *
	 * @param  ctx             current context
	 * @param  opts            client options
	 * @return                 child context to which the new client is attached
	 * @throws VeyronException if a new client cannot be created
	 */
	public VContext setNewClient(VContext ctx, Options opts) throws VeyronException;

	/**
	 * Returns the client attached to the given context.
	 *
	 * @param  ctx current context
	 * @return     the client attached to the given context
	 */
	public Client getClient(VContext ctx);

	/**
	 * Creates a new server instance with the provided options.  A particular runtime
	 * implementation chooses which options to support, but at the minimum it must handle
	 * the following options:
	 *     {@code OptionDefs.DEFAULT_LISTEN_SPEC}
	 *
	 * @param  ctx             current context
	 * @param  opts            server options
	 * @return                 the new server instance
	 * @throws VeyronException if a new server cannot be created
	 */
	public Server newServer(VContext ctx, Options opts) throws VeyronException;

	/**
	 * Attaches the given principal to a new context (that is derived from the given context).
	 *
	 * @param  ctx             current context
	 * @param  principal       principal to be attached
	 * @return                 child context to which the principal is attached
	 * @throws VeyronException if the principal couldn't be attached
	 */
	public VContext setPrincipal(VContext ctx, Principal principal) throws VeyronException;

	/**
	 * Returns the principal attached to the given context.
	 *
	 * @param  ctx current context
	 * @return     the principal attached to the given context
	 */
	public Principal getPrincipal(VContext ctx);

	/**
	 * Creates a new namespace instance and attaches it to a new context.
	 *
	 * @param  ctx             current context
	 * @param  roots           roots of the new namespace
	 * @return                 child context to which the principal is attached
	 * @throws VeyronException if the namespace couldn't be created
	 */
	public VContext setNewNamespace(VContext ctx, String... roots) throws VeyronException;

	/**
	 * Returns the namespace attached to the given context.
	 *
	 * @param  ctx current context
	 * @return     the namespace attached to the given context.
	 */
	public Namespace getNamespace(VContext ctx);

	/**
	 * Returns the base context associated with the runtime.
	 *
	 * @return the base context associated with the runtime.
	 */
	@Deprecated
	public VContext getContext();
}