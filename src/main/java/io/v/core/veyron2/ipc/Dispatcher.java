package io.v.core.veyron2.ipc;

import io.v.core.veyron2.verror2.VException;

/**
 * Dispatcher defines the interface that a server must implement to handle method invocations on
 * named objects.
 */
public interface Dispatcher {
	/**
	 * Returns the container storing (1) the service object identified by the given suffix on which
	 * methods will be served and (2) the authorizer which allows control over authorization checks.
	 * Returning a {@code null} container indicates that this dispatcher does handle the object -
	 * the framework should try other dispatchers.
	 *
	 * A thrown exception indicates the dispatch lookup has failed.  The error will be delivered
	 * back to the client and no further dispatch lookups will be performed.
	 *
	 * This method may be invoked concurrently by the underlying RPC system and hence must be
	 * thread-safe.
	 *
	 * @param  suffix          the object's name suffix.
	 * @return                 a container storing (1) the service object identified by the given
	 *                         suffix and (2) the associated authorizer; {@code null} is returned
	 *                         if this dispatcher doesn't handle the object.
	 * @throws VException      if the lookup error has occured.
	 */
	public ServiceObjectWithAuthorizer lookup(String suffix) throws VException;
}