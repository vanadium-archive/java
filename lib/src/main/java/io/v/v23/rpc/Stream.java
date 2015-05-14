// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

import io.v.v23.verror.VException;

import java.io.EOFException;
import java.lang.reflect.Type;

/**
 * The interface for a bidirectional FIFO stream of typed values.
 */
public interface Stream {
    /**
     * Places the item onto the output stream, blocking if there is no bufferspace available.
     *
     * @param  item            an item to be sent
     * @param  type            type of the provided item
     * @throws VException      if there was an error sending the item
     */
    void send(Object item, Type type) throws VException;

    /**
     * Returns the next item in the input stream, blocking until an item is available.
     * An {@link java.io.EOFException} will be thrown if a graceful end of input has been reached.
     *
     * @param  type            type of the returned item
     * @return                 the returned item
     * @throws EOFException    if a graceful end of input has been reached
     * @throws VException      if there was an error receving an item
     */
    Object recv(Type type) throws EOFException, VException;
}