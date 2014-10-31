package io.veyron.veyron.veyron2.security;

import io.veyron.veyron.veyron2.ipc.VeyronException;

/**
 * CaveatValidator is the interface for validating the restrictions specified in a caveat.
 */
public interface CaveatValidator {
	/**
	 * Returns {@code null} iff the restriction encapsulated in the corresponding caveat has
	 * been satisfied by the provided context.
	 *
	 * @param  context         the context matched against the caveat.
	 * @throws VeyronException
	 */
	public void validate(Context context) throws VeyronException;
}