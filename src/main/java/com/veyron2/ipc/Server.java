package com.veyron2.ipc;

/**
 * Server defines the interface for managing a collection of services.
 */
public interface Server {
	/**
 	 * Creates a listening network endpoint for the Server. This method may be called multiple times
 	 * to listen on multiple endpoints.  The returned endpoint represents an address that will be
 	 * published with the mount table when {@link #serve} is called.
 	 * The protocol must be one of "tcp", "tcp4", "tcp6", "unix", "unixpacket", "bluetooth", or
 	 * "veyron."
 	 * For "tcp*" protocols, addresses have the form host:port. If host is a literal IPv6 address or
 	 * host name, it must be enclosed in square brackets as in "[::1]:80", "[ipv6-host]:http" or
 	 * "[ipv6-host%zone]:80".  Empty host name means the local host.  Port number 0 means pick the
 	 * first available port.
 	 * For "bluetooth" protocol, addresses have the form MAC/port.  Port number 0 means pick the
 	 * first available port.
 	 * For "veyron" protocol, the address can be either (1) a formatted Veyron endpoint, or (2)
 	 * an object name which resolves to an endpoint.
 	 * TODO(spetrovic): Return an Endpoint object instead of a string.
	 *
	 * @param  protocol        a network protocol to be used (e.g., "tcp", "bluetooth")
	 * @param  address         an address to be used (e.g., "192.168.8.1:12")
	 * @return                 the endpoint string.
	 * @throws VeyronException if the provided protocol/address can't be listened on.
	 */
	public String listen(String protocol, String address) throws VeyronException;

	/**
	 * Performs the following two related functions:
	 *
	 * 1. Publishes all of the endpoints created via preceding calls to {@link #listen} to the mount
	 * table under <code>name</code>.  (Thereafter, resolving <code>name</code> via the mount
	 * table will return these endpoints.)
	 * 2. Associates a dispatcher to handle RPC invocations received on those endpoints.
	 *
	 * Serve may be called multiple times with different names to publish the same set of endpoints
	 * under a different name in the mount table.  The dispatcher may not be changed once it has
	 * been set to a non-nil value.  Subsequent calls to Serve should pass in either the original
	 * value of the dispatcher or nil. It is considered an error to call Listen after Serve.
	 * If name is an empty string, no attempt will made to publish that name to a mount table.
	 *
	 * @param  name            name the server should be published under.
	 * @param  dispatcher      dispatcher to handle RPC invocations.
	 * @throws VeyronException if the name can't be published of the dispatcher associated.
	 */
	public void serve(String name, Dispatcher dispatcher) throws VeyronException;

	/**
	 * Returns the rooted names that this server's endpoints have been published as (via calls to
	 * {@link #serve}).
	 *
	 * @return                 an array of rooted names that this server's endpoints have been
	 *                         published as.
	 * @throws VeyronException if an error occurs (e.g., stop() has already been invoked).
	 */
	public String[] getPublishedNames() throws VeyronException;

	/**
	 * Gracefully stops all services on this Server.  New calls are rejected, but any in-flight
	 * calls are allowed to complete.  All published mountpoints are unmounted.  This call waits for
	 * this process to complete, and returns once the server has been shut down.
	 *
	 * @throws VeyronException if there was an error stopping the server.
	 */
	public void stop() throws VeyronException;
}