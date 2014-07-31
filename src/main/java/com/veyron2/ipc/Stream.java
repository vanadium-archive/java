package com.veyron2.ipc;

import com.google.common.reflect.TypeToken;
import java.io.EOFException;

/**
 * Stream defines the interface for a bidirectional FIFO stream of typed values.
 */
public interface Stream {
	/**
	 * Places the item onto the output stream, blocking if there is no buffer
	 * space available.
	 *
	 * @param  item  an item to be sent
	 * @throws VeyronException if there was an error sending the item
	 */
	public void send(Object item) throws VeyronException;

	/**
	 * Returns the next item in the input stream, blocking until an item is available.
	 * An EOFException will be thrown if a graceful end of input has been reached.
	 *
	 * @param  type            class definition for the returned item
	 * @return Object          the returned item
	 * @throws EOFException    if a graceful end of input has been reached
	 * @throws VeyronException if there was an error receving an item
	 */
	public Object recv(TypeToken<?> type) throws EOFException, VeyronException;
}