package io.v.core.veyron2.ipc;

import io.v.core.veyron2.InputChannel;
import io.v.core.veyron2.verror.VException;

/**
 * Server defines the interface for managing a collection of services.
 */
public interface Server {
	/**
	 * Creates a listening network endpoint for the server as specified by the provided
	 * {@code ListenSpec} parameter.
	 *
	 * Returns the set of endpoints that can be used to reach this server.  A single listen
	 * address in the listen spec can lead to multiple such endpoints (e.g. :0 on a device with
	 * multiple interfaces or that is being proxied). In the case where multiple listen addresses
	 * are used it is not possible to tell which listen address supports which endpoint; if there
	 * is need to associate endpoints with specific listen addresses then {@code listen} should be
	 * called separately for each one.
	 *
	 * If the provided listen spec is {@code null}, default listen spec is used.
	 *
	 * @param  spec            information on how to create the network endpoint(s)
	 * @return                 array of endpoint strings
	 * @throws VException      if the server couldn't listen  provided protocol can't be listened on
	 */
	public String[] listen(ListenSpec spec) throws VException;

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
	 * @param  name            name under which the supplied object should be published
	 * @param  object   object to be published under the given name
	 * @throws VException      if the object couldn't be published under the given name
	 */
	public void serve(String name, Object object) throws VException;

	/**
	 * Adds the specified name to the mount table for the object or {@code Dispatcher} served by
	 * this server.
	 *
	 * This method may be called multiple times but only after {@code serve} has been called.
	 *
	 * @param  name            name to be added to the mount table
	 * @throws VException      if the name couldn't be added to the mount table
	 */
	public void addName(String name) throws VException;

	/**
	 * Removes the specified name from the mount table.
	 *
	 * This method may be called multiple times but only after {@code serve} has been called.
	 *
	 * @param name name to be removed from the mount table
	 */
	public void removeName(String name);

	/**
	 * Returns the current status of the server, see {@code ServerStatus} for details.
	 *
	 * @return the current status of the server
	 */
	public ServerStatus getStatus();

	/**
	 * Returns a channel over which {@code NetworkChange} objects will be sent. The server will
	 * not block sending data over this channel and hence change events may be lost if the
	 * implementation doesn't ensure there is sufficient buffering in the channel.
	 *
	 * @return a channel over which {@code NetworkChange} objects will be sent
	 */
	public InputChannel<NetworkChange> watchNetwork();

	/**
	 * Unregisters a channel previously registered using {@code watchNetwork}.
	 *
	 * @param channel a channel previously registered using {@code watchNetwork}
	 */
	public void unwatchNetwork(InputChannel<NetworkChange> channel);

	/**
	 * Gracefully stops all services on this server.  New calls are rejected, but any in-flight
	 * calls are allowed to complete.  All published mountpoints are unmounted.  This call waits for
	 * this process to complete, and returns once the server has been shut down.
	 *
	 * @throws VException      if there was an error stopping the server
	 */
	public void stop() throws VException;
}