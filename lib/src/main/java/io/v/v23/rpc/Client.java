// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import io.v.v23.Options;
import io.v.v23.context.VContext;

import java.lang.reflect.Type;

import javax.annotation.CheckReturnValue;

/**
 * The interface for making RPC calls.  There may be multiple outstanding calls associated with a
 * single client, and a client may be used by multiple threads concurrently.
 */
public interface Client {
    /**
     * Starts an asynchronous call of the method on the server instance identified by name with the
     * given input args (of any arity) and provided options.
     * <p>
     * A particular implementation of this interface chooses which options to support,
     * but at the minimum it must handle the following pre-defined options:
     * <ul>
     *     <li>{@link io.v.v23.OptionDefs#SKIP_SERVER_ENDPOINT_AUTHORIZATION}</li>
     * </ul>
     *
     * @param  context   client context
     * @param  name      name of the server
     * @param  method    name of the server's method to be invoked
     * @param  args      arguments to the server method.
     * @param  argTypes  types of the provided arguments
     * @param  opts      call options
     * @return           a new {@link ListenableFuture} whose result is the call object that
     *                   manages streaming args and results
     */
    @CheckReturnValue
    ListenableFuture<ClientCall> startCall(VContext context, String name, String method,
                                           Object[] args, Type[] argTypes, Options opts);

    /**
     * A shortcut for {@link #startCall(VContext, String, String, Object[], Type[], Options)} with
     * a {@code null} options parameter.
     */
    @CheckReturnValue
    ListenableFuture<ClientCall> startCall(VContext context, String name, String method,
                                           Object[] args, Type[] argTypes);

    /**
     * Discards the state associated with this client.  In-flight calls may be terminated with
     * an error.
     * <p>
     * This is a non-blocking method.
     */
    void close();
}