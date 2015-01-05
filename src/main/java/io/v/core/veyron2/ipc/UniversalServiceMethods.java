package io.v.core.veyron2.ipc;

import io.v.core.veyron2.Options;
import io.v.core.veyron2.VeyronException;
import io.v.core.veyron2.context.Context;

/**
 * UniversalServiceMethods defines the set of methods that are implemented on all services.
 */
public interface UniversalServiceMethods {
	/**
	 * Returns a description of the service.
	 *
	 * @param  context         client context for the call.
	 * @return                 description of the service.
	 * @throws VeyronException if the description couldn't be fetched.
	 */
	public ServiceSignature getSignature(Context context) throws VeyronException;


	/**
	 * Returns a description of the service.
	 *
	 * @param  context         client context for the call.
	 * @param  opts            call options.
	 * @return                 description of the service.
	 * @throws VeyronException if the description couldn't be fetched.
	 */
	public ServiceSignature getSignature(Context context, Options opts) throws VeyronException;
}