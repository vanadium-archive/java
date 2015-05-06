// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23;

import io.v.v23.verror.VException;

/**
 * Represents the write-end of a channel of T. Calls to {@link #writeValue} may block until the
 * receiver has read the value.
 */
public interface OutputChannel<T> extends AutoCloseable {
    /**
     * Writes the given value to the channel. Implementations of this method may block until the
     * receiver has read the value.
     *
     * @param value the value to write
     * @throws VException if there was an error writing to the channel
     */
    void writeValue(T value) throws VException;

    /**
     * Closes the output channel. No more values may be written to it after this call returns.
     *
     * @throws VException if there was an error closing the channel
     */
    void close() throws VException;
}
