// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.chat;

import io.v.v23.context.VContext;
import io.v.v23.verror.VException;
import io.v.x.chat.vdl.ChatClient;
import io.v.x.chat.vdl.ChatClientFactory;

/**
 * Represents a participant in a chat channel.
 */
public class Participant {
    private final String name;
    private final String endpoint;

    Participant(String name, String endpoint) {
        this.name = name;
        this.endpoint = endpoint;
    }

    public String getName() {
        return name;
    }

    public String getEndpoint() {
        return endpoint;
    }

    /**
     * Sends a message to the participant.
     */
    public void sendMessage(VContext context, String message) throws VException {
        ChatClient client = ChatClientFactory.getChatClient("/" + endpoint);
        client.sendMessage(context, message);
    }

    @Override
    public String toString() {
        return name + " (" + endpoint + ")";
    }
}
