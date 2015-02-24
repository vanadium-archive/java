package io.v.v23.vdl;

import io.v.v23.verror.VException;

/**
 * ClientStream defines a bidirectional stream with send/receive/finish arguments
 * of the specified type.
**/
public interface ClientStream<SendT, RecvT, FinishT> extends Stream<SendT, RecvT> {
  /**
   * Closes the stream, returning the final stream result.
   *
   * @return FinishT         the final stream result.
   * @throws VException      if there was an error closing the stream.
   */
  public FinishT finish() throws VException;
}