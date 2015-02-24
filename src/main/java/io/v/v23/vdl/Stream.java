package io.v.v23.vdl;

import io.v.v23.verror.VException;
import java.io.EOFException;

/**
 * Stream interfice defines a bidirectional stream with send/receive arguments
 * of the specified type.
**/
public interface Stream<SendT, RecvT> {
  /**
   * Places the item onto the output stream, blocking if there is no buffer
   * space available.
   *
   * @param  item            an item to be sent
   * @throws VException      if there was an error sending the item.
   */
  public void send(SendT item) throws VException;

  /**
   * Returns the next item in the input stream, blocking until an item is available.
   * An EOFException will be thrown if a graceful end of input has been reached.
   *
   * @return RecvT           next item in the input stream
   * @throws EOFException    if a graceful end of input has been reached
   * @throws VException      if there was an error receiving an item
   */
  public RecvT recv() throws EOFException, VException;
}
