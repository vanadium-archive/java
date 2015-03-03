package io.v.v23.security;

import io.v.v23.verror.VException;

/**
 * Authorizer is the interface for performing authorization checks.
 */
public interface Authorizer {
    /**
     * Performs authorization checks on the provided call, throwing a VException
     * iff the checks fail.
     *
     * @param  call            a call to be authorized.
     * @throws VException      iff the call isn't authorized.
     */
    public void authorize(Call call) throws VException;
}