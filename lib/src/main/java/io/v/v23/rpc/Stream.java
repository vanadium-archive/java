// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.reflect.Type;

import javax.annotation.CheckReturnValue;

/**
 * The interface for a bidirectional FIFO stream of typed values.
 */
public interface Stream {
    /**
     * Places the item onto the output stream.
     *
     * @param  item  an item to be sent
     * @param  type  type of the provided item
     */
    @CheckReturnValue
    ListenableFuture<Void> send(Object item, Type type);

    /**
     * Returns a new {@link ListenableFuture} whose result is the next item in the stream.
     * <p>
     * The returned {@link ListenableFuture} will fail if there was an error fetching the next
     * item;  {@link io.v.v23.verror.EndOfFileException} means that a graceful end of input has been
     * reached.
     *
     * @param  type type of the returned item
     */
    @CheckReturnValue
    ListenableFuture<Object> recv(Type type);
}