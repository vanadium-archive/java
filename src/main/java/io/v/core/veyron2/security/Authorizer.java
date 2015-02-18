package io.v.core.veyron2.security;

import io.v.core.veyron2.verror.VException;

/**
 * Authorizer is the interface for performing authorization checks.
 */
public interface Authorizer {
    /**
     * Performs authorization checks on the provided context, throwing a VException
     * iff the checks fail.
     *
     * @param  context         a context to be authorized.
     * @throws VException      iff the context isn't authorized.
     */
    public void authorize(VContext context) throws VException;
}