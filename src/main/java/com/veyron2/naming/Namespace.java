package com.veyron2.naming;

import com.veyron2.ipc.Context;
import com.veyron2.ipc.VeyronException;

import com.veyron2.InputChannel;

/**
 * Namespace provides translation from object names to server object addresses.
 * It represents the interface to a client side library for the MountTable
 * service.
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
	public InputChannel<MountEntry> glob(Context context, String pattern) throws VeyronException;
}