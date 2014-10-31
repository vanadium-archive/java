package io.veyron.veyron.veyron2.ipc;

/**
 * Server defines the interface for managing a collection of services.
 */
public interface Server {
	/**
	 * Creates a listening network endpoint for the server as specified by its ListenSpec parameter.
	 *
	 * The endpoint is chosen as the first available address/port on the device for the protocol
	 * specified in the ListenSpec.  The server automatically adapts to any changes in its network
	 * configuration by picking a different address/port on the device for the given protocol.
	 * The call returns the first established network endpoint.
	 *
	 * If the ListenSpec provides a non-empty proxy address, the server will connect to the proxy
	 * in order to proxy connections.
	 *
	 * If the provided ListenSpec is {@code null}, default ListenSpec is used.
	 *
	 * Method {@code listen} returns the first established endpoint.  It is safe to call
	 * {@code listen} multiple times.
	 *
	 * @param  spec            information on how to create the network endpoint.
	 * @return                 the endpoint string.
	 * @throws VeyronException if the provided protocol can't be listened on.
	 */
	public String listen(ListenSpec spec) throws VeyronException;

	/**
	 * Performs the following two related functions:
	 *
	 * 1. Publishes all of the endpoints created via preceding calls to {@code listen()} to the
	 * mount table under {@code name}.  (Thereafter, resolving {@code name} via the mount table
	 * will return these endpoints.)
	 * 2. Associates a dispatcher to handle RPC invocations received on those endpoints.
	 *
	 * Serve may be called multiple times with different names to publish the same set of endpoints
	 * under a different name in the mount table.  The dispatcher may not be changed once it has
	 * been set to a non-nil value.  Subsequent calls to {@code serve} should pass in either the
	 * original value of the dispatcher or {@code null}. It is considered an error to call
	 * {@code listen()} after {@code serve}.
	 * If {@code name} is an empty string, no attempt will made to publish that name to a mount
	 * table.
	 *
	 * @param  name            name the server should be published under.
	 * @param  dispatcher      dispatcher to handle RPC invocations.
	 * @throws VeyronException if the name can't be published or the dispatcher associated.
	 */
	public void serve(String name, Dispatcher dispatcher) throws VeyronException;

	/**
	 * Returns the rooted names that this server's endpoints have been published as (via calls to
	 * {@code serve()}).
	 *
	 * @return                 an array of rooted names that this server's endpoints have been
	 *                         published as.
	 * @throws VeyronException if an error occurs (e.g., {@code stop()} has already been invoked).
	 */
	public String[] getPublishedNames() throws VeyronException;

	/**
	 * Gracefully stops all services on this server.  New calls are rejected, but any in-flight
	 * calls are allowed to complete.  All published mountpoints are unmounted.  This call waits for
	 * this process to complete, and returns once the server has been shut down.
	 *
	 * @throws VeyronException if there was an error stopping the server.
	 */
	public void stop() throws VeyronException;
}