package io.v.core.veyron2.security;

import io.v.core.veyron2.VeyronException;
import io.v.core.veyron2.vdl.VdlValue;

/**
 * CaveatValidator is the interface for validating the restrictions specified in a caveat.
 */
public interface CaveatValidator {
	/**
	 * Returns {@code null} iff the restriction encapsulated in the corresponding caveat has
	 * been satisfied by the provided context.
	 *
	 * @param  context         the context matched against the caveat
	 * @throws VeyronException
	 */
	public void validate(VContext context) throws VeyronException;

	/**
	 * Returns the wire format for the validator.  This is the data that will be encoded
	 * in the {@code Caveat}.
	 *
	 * @return wire format for the caveat validator
	 */
	public VdlValue getWire();
}