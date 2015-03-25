// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23;

import io.v.v23.verror.VException;

import java.io.EOFException;

/**
 * InputChannel represents a stream of values of the provided type.
 */
public interface InputChannel<T> {
    /**
     * Returns true iff the next value can be read without blocking.
     *
     * @return true iff the next value can be read without blocking.
     */
    public boolean available();

    /**
     * Reads the next value from the channel, blocking if the value is unavailable.
     *
     * @return                 the next value from the channel.
     * @throws EOFException    if the graceful EOF is reached.
     * @throws VException      if a read error is encountered.
     */
    public T readValue() throws EOFException, VException;
}