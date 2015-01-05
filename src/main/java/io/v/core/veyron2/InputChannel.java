package io.v.core.veyron2;

import io.v.core.veyron2.VeyronException;

import java.io.EOFException;

/**
 * InputChannel represents a stream of values of the provided type.
 */
public interface InputChannel<T> {
	/**
	 * Returns true iff the next value can be read without blocking.
	 *
	 * @return true iff the next value can be read without blocking.
	 */
	public boolean available();

	/**
	 * Reads the next value from the channel, blocking if the value is unavailable.
	 *
	 * @return                 the next value from the channel.
	 * @throws EOFException    if the graceful EOF is reached.
	 * @throws VeyronException if a read error is encountered.
	 */
	public T readValue() throws EOFException, VeyronException;
}