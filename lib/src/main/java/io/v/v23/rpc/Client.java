// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

import io.v.v23.Options;
import io.v.v23.verror.VException;
import io.v.v23.context.VContext;

import java.lang.reflect.Type;

/**
 * The interface for making RPC calls.  There may be multiple outstanding calls associated with a
 * single client, and a client may be used by multiple threads concurrently.
 */
public interface Client {
    /**
     * Starts an asynchronous call of the method on the server instance identified by name
     * with the given input args (of any arity).  The returned {@link Call} object manages streaming
     * args and results and finishes the call.
     *
     * @param  context         client context
     * @param  name            name of the server
     * @param  method          name of the server's method to be invoked
     * @param  args            arguments to the server method
     * @param  argTypes        types of the provided arguments
     * @return                 call object that manages streaming args and results
     * @throws VException      if the call cannot be started
     */
    Call startCall(VContext context, String name, String method, Object[] args, Type[] argTypes)
            throws VException;

    /**
     * Starts an asynchronous call of the method on the server instance identified by name with the
     * given input args (of any arity) and provided options.  The returned {@link Call} object
     * manages streaming args and results and finishes the call.
     * <p>
     * A particular implementation of this interface chooses which options to support,
     * but at the minimum it must handle the following pre-defined options:
     * <ul>
     *     <li>{@link io.v.v23.OptionDefs#SKIP_SERVER_ENDPOINT_AUTHORIZATION}</li>
     * </ul>
     *
     * @param  context         client context
     * @param  name            name of the server
     * @param  method          name of the server's method to be invoked
     * @param  args            arguments to the server method.
     * @param  argTypes        types of the provided arguments
     * @param  opts            call options
     * @return                 call object that manages streaming args and results
     * @throws VException      if the call cannot be started
     */
    Call startCall(VContext context, String name, String method, Object[] args, Type[] argTypes,
            Options opts) throws VException;

    /**
     * Discards the state associated with this client.  In-flight calls may be terminated with
     * an error.
     */
    void close();

    /**
     * The interface for each in-flight call on the {@link Client}.  Method {@link #finish finish}
     * must be called to finish the call; all other methods are optional.
     */
    public interface Call extends Stream {
        /**
         * Indicates to the server that no more items will be sent; server's
         * {@link StreamServerCall#recv recv} calls will throw {@link java.io.EOFException} after
         * all sent items.  Subsequent calls to {@link Call#send send} will fail.
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
        Object[] finish(Type[] types) throws VException;
    }
}