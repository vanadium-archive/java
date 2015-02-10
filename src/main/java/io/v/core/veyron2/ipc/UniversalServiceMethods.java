package io.v.core.veyron2.ipc;

import io.v.core.veyron2.Options;
import io.v.core.veyron2.verror2.VException;
import io.v.core.veyron2.context.VContext;

/**
 * UniversalServiceMethods defines the set of methods that are implemented on all services.
 */
public interface UniversalServiceMethods {
	/**
	 * Returns a description of the service.
	 *
	 * @param  context         client context for the call.
	 * @return                 description of the service.
	 * @throws VException      if the description couldn't be fetched.
	 */
	// TODO(spetrovic): Re-enable once we can import the new Signature classes.
	//public ServiceSignature getSignature(VContext context) throws VException;


	/**
	 * Returns a description of the service.
	 *
	 * @param  context         client context for the call.
	 * @param  opts            call options.
	 * @return                 description of the service.
	 * @throws VException      if the description couldn't be fetched.
	 */
	// TODO(spetrovic): Re-enable once we can import the new Signature classes.
	//public ServiceSignature getSignature(VContext context, Options opts) throws VException;
}