// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.chat;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.namespace.Namespace;
import io.v.v23.naming.GlobReply;
import io.v.v23.naming.MountEntry;
import io.v.v23.naming.MountedServer;
import io.v.v23.rpc.Dispatcher;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerCall;
import io.v.v23.rpc.ServiceObjectWithAuthorizer;
import io.v.v23.security.Blessings;
import io.v.v23.security.VSecurity;
import io.v.v23.verror.VException;
import io.v.x.chat.vdl.ChatClient;
import io.v.x.chat.vdl.ChatClientFactory;
import io.v.x.chat.vdl.ChatServer;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.List;

public class ChatChannel {
    private final String name;
    private final VContext ctx;
    private final ChatMessageListener listener;

    private Server server;

    public ChatChannel(VContext ctx, String name, ChatMessageListener listener) {
        this.ctx = ctx;
        this.name = name;
        this.listener = listener;
    }

    public void join() throws VException {
        final ChatServer chatServer = new ChatServer() {
            @Override
            public void sendMessage(VContext ctx, ServerCall call, String text) throws VException {
                String[] blessingNames = Blessings.getBlessingNames(ctx, call.security());
                String whom;
                if (blessingNames == null || blessingNames.length == 0) {
                    whom = "unknown";
                } else {
                    whom = username(blessingNames[0]);
                }

                listener.messageReceived(whom, text);
            }
        };

        Dispatcher dispatcher = new Dispatcher() {
            @Override
            public ServiceObjectWithAuthorizer lookup(String suffix) throws VException {
                return new ServiceObjectWithAuthorizer(
                        chatServer, VSecurity.newAllowEveryoneAuthorizer());
            }
        };
        server = V.newServer(ctx);
        server.listen(V.getListenSpec(ctx));
        server.serve(name + "/" + "javatest1", dispatcher);
    }

    public void leave() throws VException {
        Preconditions.checkState(server != null, "can't leave a channel you haven't joined");
        server.stop();
    }

    public void sendMessage(String s) throws VException {
        VContext context = ctx.withTimeout(new Duration(3000));
        Namespace namespace = V.getNamespace(context);
        for (GlobReply reply : namespace.glob(context, name + "/" + "*")) {
            if (reply instanceof GlobReply.Entry) {
                MountEntry entry = ((GlobReply.Entry) reply).getElem();
                for (MountedServer server : entry.getServers()) {
                    ChatClient client = ChatClientFactory.getChatClient("/" + server.getServer());
                    try {
                        client.sendMessage(context, s);
                        break;
                    } catch (VException e) {
                        System.err.println("Couldn't send to " + server.getServer() + ": " + e);
                    }
                }
            }
        }
    }

    public List<String> getParticipants() throws VException {
        List<String> participants = new ArrayList<>();
        VContext context = ctx.withTimeout(new Duration(3000));
        Namespace namespace = V.getNamespace(context);
        for (GlobReply reply : namespace.glob(context, name + "/*")) {
            if (reply instanceof GlobReply.Entry) {
                MountEntry entry = ((GlobReply.Entry) reply).getElem();
                participants.add(entry.getName());
            }
        }
        return participants;
    }

    private static String username(String blessingName) {
        for (String field : Splitter.on("/").split(blessingName)) {
            if (field.contains("@")) {
                return field;
            }
        }
        return blessingName;
    }

    @Override
    public String toString() {
        return name;
    }
}
