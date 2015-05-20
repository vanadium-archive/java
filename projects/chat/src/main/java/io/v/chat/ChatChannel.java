// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.chat;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.namespace.Namespace;
import io.v.v23.naming.GlobReply;
import io.v.v23.naming.MountEntry;
import io.v.v23.naming.MountedServer;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerCall;
import io.v.v23.security.Blessings;
import io.v.v23.security.VCertificate;
import io.v.v23.security.VSecurity;
import io.v.v23.verror.VException;
import io.v.x.chat.vdl.ChatServer;

public class ChatChannel {
    private final String name;
    private final VContext ctx;
    private final ChatChannelListener listener;

    private Server server;

    public ChatChannel(ScheduledExecutorService service, VContext ctx, String name,
                       ChatChannelListener listener) {
        this.ctx = ctx;
        this.name = name;
        this.listener = listener;

        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    doUpdateParticipants();
                } catch (VException e) {
                    System.err.println("Could not update the list of chat participants");
                    e.printStackTrace();
                }
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    public void join() throws VException {
        final ChatServer chatServer = new ChatServer() {
            @Override
            public void sendMessage(VContext ctx, ServerCall call, String text) throws VException {
                String[] blessingNames = VSecurity.getRemoteBlessingNames(ctx, call.security());
                String whom;
                if (blessingNames == null || blessingNames.length == 0) {
                    whom = "unknown";
                } else {
                    whom = blessingUsername(blessingNames[0]);
                }

                listener.messageReceived(whom, text);
            }
        };

        server = V.newServer(ctx);
        server.listen(V.getListenSpec(ctx));

        // TODO(sjr): use the Namespace API to choose a better endpoint
        String mountPath = name + "/javatest";
        server.serve(mountPath, chatServer, VSecurity.newAllowEveryoneAuthorizer());
    }

    private static String blessingsName(Blessings blessings) {
        Function<VCertificate, String> extensionFunc = new Function<VCertificate, String>() {
            @Override
            public String apply(VCertificate input) {
                return input.getExtension();
            }
        };
        List<String> names = new ArrayList<>(blessings.getCertificateChains().size());
        for (List<VCertificate> chain : blessings.getCertificateChains()) {
            names.add(Joiner.on('/').join(Iterables.transform(chain, extensionFunc)));
        }
        return Joiner.on(',').join(names);
    }

    public void leave() throws VException {
        Preconditions.checkState(server != null, "can't leave a channel you haven't joined");
        server.stop();
    }

    public void sendMessage(String s) throws VException {
        VContext context = ctx.withTimeout(new Duration(3000));
        for (Participant participant : getParticipants()) {
            try {
                participant.sendMessage(context, s);
            } catch (VException e) {
                System.err.println("Could not send message to " + participant);
                e.printStackTrace();
            }
        }
    }

    private void doUpdateParticipants() throws VException {
        listener.participantsUpdated(getParticipants());
    }

    public List<Participant> getParticipants() throws VException {
        List<Participant> participants = new ArrayList<>();
        VContext context = ctx.withTimeout(new Duration(3000));
        Namespace namespace = V.getNamespace(context);
        for (GlobReply reply : namespace.glob(context, name + "/*")) {
            if (reply instanceof GlobReply.Entry) {
                MountEntry entry = ((GlobReply.Entry) reply).getElem();
                for (MountedServer server : entry.getServers()) {
                    participants
                            .add(new Participant(endpointUsername(server.getServer()), server.getServer()));
                    break;
                }
            }
        }
        return participants;
    }

    private static String blessingUsername(String blessingName) {
        for (String field : Splitter.on("/").split(blessingName)) {
            if (field.contains("@")) {
                return field;
            }
        }
        return blessingName;
    }

    private static String endpointUsername(String endpoint) {
        // TODO(sjr): replace this with Endpoint once that is written.
        Matcher matcher = Pattern.compile("@(.*)@@$").matcher(endpoint);
        if (!matcher.matches()) {
            return endpoint;
        }
        List<String> fields = Splitter.on('@').splitToList(matcher.group(1));
        if (fields.size() < 5) {
            return endpoint;
        }
        String joinedBlessings = Joiner.on('@').join(fields.subList(5, fields.size()));
        for (String blessing : Splitter.on('/').split(joinedBlessings)) {
            if (blessing.contains("@")) {
                return blessing;
            }
        }

        return endpoint;
    }

    @Override
    public String toString() {
        return name;
    }
}
