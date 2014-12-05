package io.veyron.veyron.veyron2;

import io.veyron.veyron.veyron2.context.Context;
import io.veyron.veyron.veyron2.ipc.Client;
import io.veyron.veyron.veyron2.ipc.Server;
import io.veyron.veyron.veyron2.naming.Namespace;
import io.veyron.veyron.veyron2.security.Principal;

/**
 * VRuntimeImpl is a base abstract class for all runtime implementations.
 *
 * Methods in this class aren't meant to be invoked directly; instead, {@code VRuntime}
 * should be initialized with an instance of this class, passed in through the
 * {@code OptionDefs.RUNTIME} option.
 */
public abstract class VRuntimeImpl {
	/**
	 * Creates a new client instance.
	 *
	 * @return                 the new client instance
	 * @throws VeyronException if a new client cannot be created
	 */
	protected abstract Client newClient() throws VeyronException;

	/**
	 * Creates a new client instance with the provided options.  A particular implementation chooses
	 * which options to support, but at the minimum it must handle the following options:
	 *     CURRENTLY NO OPTIONS ARE MANDATED
	 *
	 * @param  opts            client options
	 * @return                 the new client instance
	 * @throws VeyronException if a new client cannot be created
	 */
	protected abstract Client newClient(Options opts) throws VeyronException;

	/**
	 * Creates a new server instance.
	 *
	 * @return                 the new server instance
	 * @throws VeyronException if a new server cannot be created
	 */
	protected abstract Server newServer() throws VeyronException;

	/**
	 * Creates a new server instance with the provided options.  A particular implementation chooses
	 * which options to support, but at the minimum it must handle the following options:
	 *     CURRENTLY NO OPTIONS ARE MANDATED
	 *
	 * @param  opts            server options
	 * @return                 the new server instance
	 * @throws VeyronException if a new server cannot be created
	 */
	protected abstract Server newServer(Options opts) throws VeyronException;

	/**
	 * Returns the pre-configured client that is created when the runtime is initialized.
	 *
	 * @return the pre-configured client instance.
	 */
	protected abstract Client getClient();

	/**
	 * Returns a new context instance.
	 *
	 * @return context the new (client) context.
	 */
	protected abstract Context newContext();

	/**
	 * Returns the principal that represents this runtime.
	 *
	 * @return the principal that represents this runtime.
	 */
	protected abstract Principal getPrincipal();

	/**
	 * Returns the pre-configured namespace that is created when the Runtime is initialized.
	 *
	 * @return the pre-configured namespace instance.
	 */
	protected abstract Namespace getNamespace();
}