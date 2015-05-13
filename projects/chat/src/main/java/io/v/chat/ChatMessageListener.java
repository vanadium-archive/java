// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.chat;

/**
 * Instances of this class are used when constructing a {@link ChatChannel}. The channel will notify
 * these instances when messages arrive on the channel, see {@link #messageReceived}.
 */
public interface ChatMessageListener {
    /**
     * Called by the {@link ChatChannel} when a message is received. The call will be made on the
     * network request thread, so it is important that the receiver does any lengthy operations on a
     * separate thread.
     */
    void messageReceived(String whom, String message);
}
