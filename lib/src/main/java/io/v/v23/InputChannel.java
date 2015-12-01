// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * The read-end of a channel of {@code T}.
*/
public interface InputChannel<T> {
    /**
     * Returns a new {@link ListenableFuture} whose result is the next item in the channel.
     * <p>
     * The returned {@link ListenableFuture} will fail if there was an error fetching the next
     * item;  {@link io.v.v23.verror.EndOfFileException} means that a graceful end of input has been
     * reached.
     */
    ListenableFuture<T> recv();
}
