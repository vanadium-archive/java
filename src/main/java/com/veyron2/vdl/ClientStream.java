package com.veyron2.vdl;

import com.veyron2.ipc.VeyronException;

/**
 * ClientStream defines a bidirectional stream with send/receive/finish arguments
 * of the specified type.
**/
public interface ClientStream<SendT, RecvT, FinishT> extends Stream<SendT, RecvT> {
  /**
   * Closes the stream, returning the final stream result.
   *
   * @return FinishT         the final stream result.
   * @throws VeyronException if there was an error closing the stream.
   */
  public FinishT finish() throws VeyronException;
}