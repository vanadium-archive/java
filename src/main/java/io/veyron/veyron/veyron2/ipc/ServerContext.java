package io.veyron.veyron.veyron2.ipc;

import io.veyron.veyron.veyron2.context.Context;
import io.veyron.veyron.veyron2.security.Blessings;

/**
 * ServerContext defines the in-flight call state on the server, not including methods
 * to stream args and results.
 */
public interface ServerContext extends Context, io.veyron.veyron.veyron2.security.Context {
	/**
	 * Returns blessings bound to the server's private key (technically, the server principal's
	 * private key) provided by the client of the RPC.
	 *
	 * This method can return {@code null}, which indicates that the client did not provide any
	 * blessings to the server with the request.
	 *
	 * @return blessings bound to the server's private key.
	 */
	public Blessings blessings();
}