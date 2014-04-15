package com.veyron2.ipc;

import java.util.Date;

/**
 * Context defines the in-flight call state on the server, not including methods
 * to stream args and results.
 * TODO(spetrovic): add security context.
 */
public interface Context {
	/**
	 * Returns the deadline for this call.
	 *
	 * @return Date the deadline for this call.
	 */
	public Date deadline();

	/**
	 * Returns true iff the call has been cancelled.
	 *
	 * @return boolean true iff the call has been cancelled.
	 *
	 */
	public boolean cancelled();
}