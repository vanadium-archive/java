// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import io.v.v23.verror.VException;

import java.lang.reflect.Type;

/**
 * The interface for each in-flight call on the {@link Client}.  Method {@link #finish finish}
 * must be called to finish the call; all other methods are optional.
 */
public interface ClientCall extends Stream {
    /**
     * Indicates to the server that no more items will be sent; server's
     * {@link StreamServerCall#recv recv} calls will throw {@link java.io.EOFException} after
     * all sent items.  Subsequent calls to {@link Stream#send send} will fail.
     * <p>
     * This is an optional call - it's used by streaming clients that need the server to throw
     * {@link java.io.EOFException}.
     *
     * @throws VException      if there was an error closing
     */
    void closeSend() throws VException;

    /**
     * Blocks until the server has finished the call and returns the positional output arguments
     * (of any arity).
     *
     * @param  types           types for all the output arguments
     * @return                 an array of output arguments
     * @throws VException      if there was an error executing the call
     */
    ListenableFuture<Object[]> finish(Type[] types) throws VException;
}
