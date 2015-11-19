// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.vdl;

import io.v.v23.verror.VException;

/**
 * Represents the send side of the client bidirectional stream.
 *
 * @param <SendT>   type of values that the client is sending to the server
 * @param <FinishT> type of the final return value from the server
 */
public interface ClientSendStream<SendT, FinishT> {
    /**
     * Places the item onto the output stream, blocking if there is no buffer space available.
     *
     * @param  item            an item to be sent
     * @throws VException      if there was an error sending the item
     */
    void send(SendT item) throws VException;

    /**
     * Indicates to the server that no more items will be sent;  server's receiver iterator
     * will gracefully terminate after receiving all sent items.
     * <p>
     * This is an optional call - e.g. a client might call {@link #close} if it needs to continue
     * receiving items from the server after it's done sending.
     * <p>
     * Like {@link #send}, blocks if there is no buffer space available.
     *
     * @throws VException if there was an error encountered while closing, or if this method is
     *                    called after the stream has been canceled
     */
    void close() throws VException;

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
