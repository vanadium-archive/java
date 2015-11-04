// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.chat;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;
import io.v.v23.security.VPrincipal;
import io.v.v23.security.VSecurity;
import io.v.v23.security.access.AccessList;
import io.v.v23.security.access.Permissions;
import io.v.v23.services.mounttable.Constants;
import io.v.v23.verror.VException;
import io.v.x.chat.vdl.ChatServer;

public class ChatChannel {
    private final String name;
    private VContext ctx;
    private final ChatChannelListener listener;

    private static final int MAX_NAME_RETRIES = 25;
    private static final Logger logger = Logger.getLogger(ChatChannel.class.getName());

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

        String mountPath = getLockedPath(ctx);
        if (mountPath != null) {
            ctx = V.withNewServer(ctx, mountPath, chatServer,
                    VSecurity.newAllowEveryoneAuthorizer());
        } else {
            throw new VException("Could not find an appropriate path name for the chat server");
        }
    }

    /**
     * Returns a path to which a chat server may be bound, or {@code null} if no appropriate path
     * could be found.
     */
    private String getLockedPath(VContext ctx) {
        VPrincipal principal = V.getPrincipal(ctx);
        Blessings defaultBlessings = principal.blessingStore().defaultBlessings();
        List<BlessingPattern> patterns = new ArrayList<>();
        for (String blessing : VSecurity.getBlessingNames(principal, defaultBlessings)) {
             patterns.add(new BlessingPattern(blessing));
        }
        AccessList myAcl = new AccessList(patterns, ImmutableList.<String>of());
        AccessList openAcl = new AccessList(ImmutableList.of(new BlessingPattern("...")),
                ImmutableList.<String>of());
        Permissions perms = new Permissions();
        perms.put(Constants.ADMIN.getValue(), myAcl);
        perms.put(Constants.CREATE.getValue(), myAcl);
        perms.put(Constants.MOUNT.getValue(), myAcl);

        // Let anybody read and resolve the name.
        perms.put(Constants.RESOLVE.getValue(), openAcl);
        perms.put(Constants.READ.getValue(), openAcl);

        for (int i = 0; i < MAX_NAME_RETRIES; i++) {
            String path = name + "/" + UUID.randomUUID().toString();
            try {
                V.getNamespace(ctx).setPermissions(ctx, path, perms, "");
                return path;
            } catch (VException e) {
                logger.log(Level.WARNING, "retry #" + (i + 1) + " failed", e);
                // It failed, try another name!
            }
        }
        return null;
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
                    participants.add(new Participant(endpointUsername(server.getServer()),
                            server.getServer()));
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
