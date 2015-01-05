package io.v.core.veyron2.ipc;

import io.v.core.veyron2.VeyronException;

import java.io.EOFException;
import java.lang.reflect.Type;

/**
 * Stream defines the interface for a bidirectional FIFO stream of typed values.
 */
public interface Stream {
	/**
	 * Places the item onto the output stream, blocking if there is no bufferspace available.
	 *
	 * @param  item            an item to be sent.
	 * @param  type            type of the provided item.
	 * @throws VeyronException if there was an error sending the item.
	 */
	public void send(Object item, Type type) throws VeyronException;

	/**
	 * Returns the next item in the input stream, blocking until an item is available.
	 * An {@code EOFException} will be thrown if a graceful end of input has been reached.
	 *
	 * @param  type            type of the returned item.
	 * @return                 the returned item.
	 * @throws EOFException    if a graceful end of input has been reached.
	 * @throws VeyronException if there was an error receving an item.
	 */
	public Object recv(Type type) throws EOFException, VeyronException;
}