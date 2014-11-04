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
	 * Associates object with name by publishing the address of this server with the mount table
	 * under the supplied name.
	 *
	 * If the supplied object implements a {@code Dispatcher} interface, RPCs invoked on the
	 * supplied name will be delivered to the supplied dispatcher's {@code lookup} method which will
	 * in turn return the object and security authorizer used to serve the actual RPC call.
	 *
	 * If the supplied object doesn't implement a {@code Dispatcher} interface, RPCs invoked on the
	 * supplied name will be delivered directly to methods implemented by the supplied object.
	 * In this case, default security authorizer will be used.
	 *
	 * Serve may be called multiple times with different names to publish the object under different
	 * names. The object may not be changed once it has been set to a non-{@code null} value:
	 * subsequent calls to Serve should pass in either the original value of the object or
	 * {@code null}.
	 *
	 * It is considered an error to call {@code listen} after {@code serve} If the name is an
	 * empty string, no attempt will made to publish that name to a mount table.
	 *
	 * @param  name            name under which the supplied object should be published.
	 * @param  object   object to be published under the given name.
	 * @throws VeyronException if the object couldn't be published under the given name.
	 */
	public void serve(String name, Object object) throws VeyronException;

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