package io.v.v23.security;

import io.v.v23.verror.VException;

/**
 * CaveatValidator is the interface for validating the restrictions specified in a caveat.
 */
public interface CaveatValidator {
    /**
     * Throws an exception iff the restriction encapsulated in the corresponding caveat parameter
     * hasn't been satisfied given the call.
     *
     * @param  call            a call the caveat is matched against
     * @param  side            the side (local or remote) of the caller
     * @param  param           the (sole) caveat parameter
     * @throws VException      if the caveat couldn't be validated
     */
    public void validate(Call call, CallSide side, Object param) throws VException;
}
