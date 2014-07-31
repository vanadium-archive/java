package com.veyron2.security;

import com.veyron2.ipc.VeyronException;

/**
 * Authorizer is the interface for performing authorization checks.
 */
public interface Authorizer {
	/**
	 * Performs authorization checks on the provided context, throwing a VeyronException
	 * iff the checks fail.
	 *
	 * @param  context         a context to be authorized.
	 * @throws VeyronException iff the context isn't authorized.
	 */
	public void authorize(Context context) throws VeyronException;
}