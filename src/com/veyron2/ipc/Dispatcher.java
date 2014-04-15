package com.veyron2.ipc;

/**
 * Dispatcher defines the interface that a server must implement to handle
 * method invocations on named objects.
 */
public interface Dispatcher {
	/**
	 * Returns the object identified by the given suffix on which methods will be
	 * served.  Returning null indicates that this Dispatcher does handle the object -
	 * the framework should try other Dispatchers.
	 *
	 * An Authorizer is also returned to allow control over authorization checks.
	 * Returning a null Authorizer indicates the default authorization checks
	 * should be used.
	 * TODO(spetrovic): Return Authorizer for real.
	 *
	 * A thrown exception indicates the dispatch lookup has failed.  The error will
	 * be delivered back to the client and no further dispatch lookups
	 * will be performed.
	 *
	 * Lookup may be invoked concurrently by the underlying RPC system and hence
	 * must be thread-safe.
	 *
	 * @param  suffix          the object's name suffix
	 * @return Object          the object identified by the given suffix
	 * @throws VeyronException if the lookup error has occured
	 */
	public Object lookup(String suffix) throws VeyronException;
}