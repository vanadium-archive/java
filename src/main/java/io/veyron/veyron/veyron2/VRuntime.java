package io.veyron.veyron.veyron2;

import io.veyron.veyron.veyron2.ipc.Client;
import io.veyron.veyron.veyron2.context.Context;
import io.veyron.veyron.veyron2.ipc.Server;
import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.naming.Namespace;
import io.veyron.veyron.veyron2.security.Principal;

/**
 * VRuntime represents the local environment allowing clients and servers to communicate
 * with one another.
 */
public interface VRuntime {
	/**
	 * Creates a new client instance.
	 *
	 * @return                 the new client instance
	 * @throws VeyronException if a new client cannot be created
	 */
	public Client newClient() throws VeyronException;

	/**
	 * Creates a new client instance with the provided options.  A particular implementation of this
	 * interface chooses which options to support, but at the minimum it must handle the following
	 * options:
	 *     CURRENTLY NO OPTIONS ARE MANDATED
	 *
	 * @param  opts            client options
	 * @return                 the new client instance
	 * @throws VeyronException if a new client cannot be created
	 */
	public Client newClient(Options opts) throws VeyronException;

	/**
	 * Creates a new server instance.
	 *
	 * @return                 the new server instance
	 * @throws VeyronException if a new server cannot be created
	 */
	public Server newServer() throws VeyronException;

	/**
	 * Creates a new server instance with the provided options.  A particular implementation of this
	 * interface chooses which options to support, but at the minimum it must handle the following
	 * options:
	 *     CURRENTLY NO OPTIONS ARE MANDATED
	 *
	 * @param  opts            server options
	 * @return                 the new server instance
	 * @throws VeyronException if a new server cannot be created
	 */
	public Server newServer(Options opts) throws VeyronException;

	/**
	 * Returns the pre-configured client that is created when the runtime is initialized.
	 *
	 * @return the pre-configured client instance.
	 */
	public Client getClient();

	/**
	 * Returns a new context instance.
	 *
	 * @return context the new (client) context.
	 */
	public Context newContext();

	/**
	 * Returns the principal that represents this runtime.
	 *
	 * @return the principal that represents this runtime.
	 */
	public Principal getPrincipal();

	/**
	 * Returns the pre-configured Namespace that is created when the Runtime is initialized.
	 *
	 * @return the pre-configured Namespace instance.
	 */
	public Namespace getNamespace();
}