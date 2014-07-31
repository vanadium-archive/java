package com.veyron2.security;

import com.veyron2.ipc.VeyronException;

/**
 * Caveat is the interface for restrictions on the scope of an identity. These
 * restrictions are validated by the remote end when a connection is made using the
 * identity.
 */
public interface Caveat {
	/**
	 * Tests the restrictions specified in the caveat under the provided context, throwing a
	 * VeyronException iff they aren't satisfied.
	 *
	 * @param  context         a context to be validated.
	 * @throws VeyronException iff the caveat restrictions aren't satisfied.
	 */
	public void validate(Context context) throws VeyronException;
}