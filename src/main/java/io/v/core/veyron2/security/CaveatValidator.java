package io.v.core.veyron2.security;

import io.v.core.veyron2.verror2.VException;
import io.v.core.veyron2.vdl.VdlValue;

/**
 * CaveatValidator is the interface for validating the restrictions specified in a caveat.
 */
public interface CaveatValidator {
	/**
	 * Throws an exception iff the restriction encapsulated in the corresponding caveat parameter
	 * hasn't been satisfied by the provided context.
	 *
	 * @param  context         the context matched against the caveat
	 * @param  param           the (sole) caveat parameter 
	 * @throws VException      if the caveat couldn't be validated
	 */
	public void validate(VContext context, Object param) throws VException;
}