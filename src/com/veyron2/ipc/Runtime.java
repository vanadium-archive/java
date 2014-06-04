package com.veyron2.ipc;

/**
 * Runtime represents the local environment allowing clients and servers to communicate
 * with one another.
 */
public interface Runtime {
	/**
	 * Creates a new Client instance.
	 * TODO(spetrovic): type-restrict the passed-in options.
	 *
	 * @param  opts            client-creation options
	 * @return Client          the new client instance
	 * @throws VeyronException if a new client cannot be created
	 */
	public Client newClient(Object... opts) throws VeyronException;

	/**
	 * Creates a new Server instance.
	 * TODO(spetrovic): type-restrict the passed-in options.
	 *
	 * @param  opts            server-creation options
	 * @return Server          the new server instance
	 * @throws VeyronException if a new server cannot be created
	 */
	public Server newServer(Object... opts) throws VeyronException;

	/**
	 * Returns the pre-configured Client that is created when the
	 * Runtime is initialized.
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