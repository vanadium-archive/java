// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.vdl;

import io.v.v23.verror.VException;

/**
 * Represents the send side of the server bidirectional stream.
 *
 * @param <SendT>   type of values that the server is sending to the client
 */
public interface ServerSendStream<SendT> {
    /**
     * Places the item onto the output stream, blocking if there is no buffer space available.
     *
     * @param  item            an item to be sent
     * @throws VException      if there was an error sending the item
     */
    void send(SendT item) throws VException;
}
