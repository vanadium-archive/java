// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.vdl;

import io.v.v23.verror.VException;

/**
 * Represents the clients side of the established bidirectional stream.
 *
 * @param <SendT>   type of values that the client is sending to the server
 * @param <RecvT>   type of values that the client is receiving from the server
 * @param <FinishT> type of the final return value from the server
 */
public interface ClientStream<SendT, RecvT, FinishT>
        extends ClientSendStream<SendT, FinishT>, ClientRecvStream<RecvT, FinishT> {
    /**
     * Performs the equivalent of {@link #close}, then blocks until the server is
     * done and returns the return value for the call.
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
