// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.vdl;

import com.google.common.util.concurrent.ListenableFuture;

import javax.annotation.CheckReturnValue;

import io.v.v23.OutputChannel;

/**
 * Represents the send side of the client bidirectional stream.
 *
 * @param <SendT>   type of values that the client is sending to the server
 * @param <FinishT> type of the final return value from the server
 */
public interface ClientSendStream<SendT, FinishT> extends OutputChannel<SendT> {
    /**
     * Returns a new {@link ListenableFuture} that performs the equivalent of {@link #close},
     * then waits until the server is done and returns the call return value.
     * <p>
     * If the call context has been canceled, depending on the timing, the returned
     * {@link ListenableFuture} may either fail with {@link io.v.v23.verror.CanceledException} or
     * return a valid call return value.
     * <p>
     * Calling {@link #finish} is mandatory for releasing stream resources, unless the call context
     * has been canceled or any of the other methods threw an exception.
     * <p>
     * Must be called at most once.
     *
     * @return a new {@link ListenableFuture} whose result is the call return value
     */
    @CheckReturnValue
    ListenableFuture<FinishT> finish();
}
