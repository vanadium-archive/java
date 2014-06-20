package com.veyron2;

import com.veyron2.ipc.Client;
import com.veyron2.ipc.Context;
import com.veyron2.ipc.Server;
import com.veyron2.ipc.VeyronException;

import java.util.Map;

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
	 * Creates a new Client instance with the provided options specified as (key, value) pairs.
	 * A particular implementation of this interface chooses which options to support,
	 * but at the minimum it must handle the following options:
	 * {@link com.veyron2.Options#CALL_TIMEOUT}
	 *
	 * @param  options         client options
	 * @return Client          the new client instance
	 * @throws VeyronException if a new client cannot be created
	 */
	public Client newClient(Map<String, Object> options) throws VeyronException;

	/**
	 * Creates a new Server instance.
	 *
	 * @return Server          the new server instance
	 * @throws VeyronException if a new server cannot be created
	 */
	public Server newServer() throws VeyronException;

	/**
	 * Creates a new Server instance with the provided options, specified as (key, value) pairs.
	 * A particular implementation of this interface chooses which options to support.
	 *
	 * @param  options         server options
	 * @return Server          the new server instance
	 * @throws VeyronException if a new server cannot be created
	 */
	public Server newServer(Map<String, Object> options) throws VeyronException;

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