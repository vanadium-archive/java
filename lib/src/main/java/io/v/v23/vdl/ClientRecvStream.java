// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.vdl;

import io.v.v23.VIterable;
import io.v.v23.verror.VException;

/**
 * Represents the receiving side of the client bidirectional stream.
 *
 * @param <RecvT>   type of values that the client is receiving from the server
 * @param <FinishT> type of the final return value from the server
 */
public interface ClientRecvStream<RecvT, FinishT> extends VIterable<RecvT> {
    /**
     * Blocks until the server is done and returns the call return value.
     * <p>
     * Doesn't block if the call has been canceled; depending on the timing, {@link #finish} may
     * either throw an exception signaling cancellation or return the valid return value from the
     * server.
     * <p>
     * Calling {@link #finish} is mandatory for releasing stream resources, unless the call
     * has been canceled or any of the other methods throw an exception.
     * <p>
     * Must be called at most once.
     *
     * @return FinishT         the final stream result
     * @throws VException      if there was an error closing the stream
     */
    FinishT finish() throws VException;
}
