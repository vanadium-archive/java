package io.v.core.veyron2.naming;

import io.v.core.veyron2.context.VContext;
import io.v.core.veyron2.VeyronException;

import io.v.core.veyron2.InputChannel;

/**
 * Namespace provides translation from object names to server object addresses.  It represents the
 * interface to a client side library for the MountTable service.
 */
public interface Namespace {
	/**
	 * Returns all names matching the provided pattern.
	 *
	 * @param  context         a client context.
	 * @param  pattern         a pattern that should be matched.
	 * @return                 an input channel of MountEntry objects matching the provided pattern.
	 * @throws VeyronException if an error is encountered.
	 */
	public InputChannel<VDLMountEntry> glob(VContext context, String pattern) throws VeyronException;
}