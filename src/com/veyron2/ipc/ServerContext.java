package com.veyron2.ipc;

import com.veyron2.security.PublicID;
import java.util.Date;

/**
 * ServerContext defines the in-flight call state on the server, not including methods
 * to stream args and results.
 * TODO(spetrovic): add security context.
 */
public interface ServerContext extends Context, com.veyron2.security.Context {
	/**
	 * Blessing is a credential provided by the client bound to the private key of the server's
	 * identity. It can be nil, in which case the client did not provide any additional credentials.
	 *
	 * @return client's credential.
	 */
	public PublicID blessing();

	/**
	 * Returns the deadline for this call.
	 *
	 * @return Date the deadline for this call.
	 */
	public Date deadline();

	/**
	 * Returns true iff the call has been cancelled or otherwise closed.
	 *
	 * @return boolean true iff the call has been cancelled or otherwise closed.
	 *
	 */
	public boolean closed();
}