// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

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
   * @return FinishT         the final stream result
   * @throws VException      if there was an error closing the stream
   */
  FinishT finish() throws VException;
}