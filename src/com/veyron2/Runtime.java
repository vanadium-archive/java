package com.veyron2;

import com.veyron2.ipc.Client;
import com.veyron2.ipc.Context;
import com.veyron2.ipc.Server;
import com.veyron2.ipc.VeyronException;

/**
 * Runtime represents the local environment allowing clients and servers to communicate
 * with one another.
 */
public interface Runtime {
	/**
	 * Creates a new Client instance.
	 *
	 * @return Client          the new client instance
	 * @throws VeyronException if a new client cannot be created
	 */
	public Client newClient() throws VeyronException;

    /**
	 * Creates a new Client instance with the provided options.  A particular implementation of this
	 * interface chooses which options to support, but at the minimum it must handle the following
	 * options:
	 * {@link com.veyron2.OptionDefs#CALL_TIMEOUT}
	 *
	 * @param  opts            client options
	 * @return Client          the new client instance
	 * @throws VeyronException if a new client cannot be created
	 */
	public Client newClient(Options opts) throws VeyronException;

	/**
	 * Creates a new Server instance.
	 *
	 * @return Server          the new server instance
	 * @throws VeyronException if a new server cannot be created
	 */
	public Server newServer() throws VeyronException;

	/**
	 * Creates a new Server instance with the provided options.  A particular implementation of this
	 * interface chooses which options to support.
	 *
	 * @param  opts            server options
	 * @return Server          the new server instance
	 * @throws VeyronException if a new server cannot be created
	 */
	public Server newServer(Options opts) throws VeyronException;

	/**
	 * Returns the pre-configured Client that is created when the Runtime is initialized.
	 *
	 * @return Client the pre-configured client instance.
	 */
	public Client getClient();

	/**
	 * Returns a new Context instance.
	 *
	 * @return Context the new (client) context.
	 */
	public Context newContext();
}