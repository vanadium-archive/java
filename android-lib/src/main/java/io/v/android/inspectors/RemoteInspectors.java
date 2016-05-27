// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.inspectors;

import android.util.Base64;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.ReadableDuration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.List;

import io.v.android.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.naming.Endpoint;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.PublisherEntry;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerStatus;
import io.v.v23.security.Blessings;
import io.v.v23.security.Caveat;
import io.v.v23.security.VPrincipal;
import io.v.v23.security.VSecurity;
import io.v.v23.security.access.Constants;
import io.v.v23.security.access.Tag;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * A RemoteInspectors object creates and manages a secure network server that allows authorized
 * remote users to inspect the (Vanadium-specific) state of the application such as statistics,
 * and log files.
 *
 * <p>When a remote user is invited via the {@link #invite(String, ReadableDuration)} method, a
 * principal and blessing is generated that is suitable for use by the
 * <a href="https://godoc.org/v.io/x/ref/services/debug/debug">{@code debug browse}</a> command.
 *
 * By default (if no tags are provided to the constructor), the remote user will not be able
 * to invoke any methods that involve reading or writing of application data, instead they
 * will be limited to methods that provide debugging metadata only. To enable remote users
 * to invoke read and/or write methods, provide
 * {@link io.v.v23.security.access.Constants#READ} and/or
 * {@link io.v.v23.security.access.Constants#WRITE} to the constructor.
 */
public class RemoteInspectors {
    private static final String TAG = "RemoteInspectors";
    private static final int BASE64_FLAGS = Base64.URL_SAFE | Base64.NO_WRAP;
    private VContext mCtx;
    private List<Tag> mTags;

    /**
     * Creates a secure network server to expose application state to invited remote users.
     *
     * @param ctx the Vanadium context of the application whose state is to be exposed
     * @params tags The set of method tags that determine which methods an invited user can invoke.
     * The {@link io.v.v23.security.access.Constants#DEBUG} and
     * {@link io.v.v23.security.access.Constants#RESOLVE} tags are always included, whether the
     * caller includes it in tags or not.
     * @throws VException
     */
    public RemoteInspectors(VContext ctx, Tag... tags) throws VException {
        // Enhance tags to ensure that it always has DEBUG and RESOLVE.
        mTags = new ArrayList<>(tags.length + 2);
        boolean hasDebug = false;
        boolean hasResolve = false;
        for (Tag t : tags) {
            if (t.equals(Constants.DEBUG)) {
                hasDebug = true;
            }
            if (t.equals(Constants.RESOLVE)) {
                hasResolve = true;
            }
            mTags.add(t);
        }
        if (!hasDebug) {
            mTags.add(Constants.DEBUG);
        }
        if (!hasResolve) {
            mTags.add(Constants.RESOLVE);
        }

        mCtx = ctx.withCancel();
        Server server = V.getServer(ctx);
        if (server == null) {
            String mountedName;
            try {
                mountedName = createMountedName(ctx);
            } catch (NoSuchAlgorithmException e) {
                throw new VException("Unable to create mounted name:" + e);
            }
            Log.i(TAG, "Application state should be inspectable via: " + mountedName);
            mCtx = V.withNewServer(
                    V.withListenSpec(mCtx, new ListenSpec("tcp", ":0").withProxy("proxy")),
                    mountedName,
                    new InspectedServer() {},
                    VSecurity.newDefaultAuthorizer());
        }
    }

    /**
     * Invite a remote user to inspect state of the application.
     *
     * @param invitee the name to refer to the remote user as (typically an email address)
     * @param duration time after which inspection privileges expire
     *
     * @return A textual description of how the remote user can access state of the running
     * application.
     */
    public synchronized String invite(String invitee, ReadableDuration duration)
        throws  VException {
        if (mCtx == null) {
            throw new VException("RemoteInspectors.stop already called");
        }

        Caveat debugOnly = VSecurity.newCaveat(
                io.v.v23.security.access.Constants.ACCESS_TAG_CAVEAT, mTags);
        Caveat expiration = VSecurity.newExpiryCaveat(DateTime.now().plus(duration));

        String privateKey;
        VPrincipal delegate;
        try {
            KeyPairGenerator e = KeyPairGenerator.getInstance("EC");
            e.initialize(256);
            KeyPair keyPair = e.generateKeyPair();
            privateKey = Base64.encodeToString(keyPair.getPrivate().getEncoded(), BASE64_FLAGS);
            delegate = VSecurity.newPrincipal(
                    VSecurity.newSigner(keyPair.getPrivate(), (ECPublicKey) keyPair.getPublic()));
        } catch (NoSuchAlgorithmException e) {
            throw new VException("Could not mint private key: " + e.getMessage());
        }
        VPrincipal me = V.getPrincipal(mCtx);
        Blessings b = me.bless(
                delegate.publicKey(),
                me.blessingStore().defaultBlessings(),
                "debugger" + io.v.v23.security.Constants.CHAIN_SEPARATOR + invitee,
                debugOnly,
                expiration);
        StringBuilder builder = new StringBuilder()
                .append("Please inspect my application using:")
                .append('\n')
                .append('\n')
                .append("debug browse")
                .append(" --key=").append(privateKey)
                .append(" --blessings=").append(Base64.encodeToString(
                        VomUtil.encode(b, b.getClass()), BASE64_FLAGS))
                .append(" ");
        return appendServerAddresses(builder).toString();
    }

    private StringBuilder appendServerAddresses(StringBuilder sb)  {
        ServerStatus status = V.getServer(mCtx).getStatus();
        for (PublisherEntry e : status.getPublisherStatus()) {
            sb = sb.append(e.getName()).append(" ");
        }
        for (Endpoint ep : status.getEndpoints()) {
            sb = sb.append(ep.name()).append(" ");
        }
        return sb;
    }

    private static String createMountedName(VContext ctx) throws NoSuchAlgorithmException {
        // TODO(ashankar): Use a conventions library here, something like
        // https://godoc.org/v.io/v23/conventions
        // What follows below is NOT worthy of any form of emulation!
        //
        // The intent is to create a unique name for this application that it will have
        // permission to mount under. The place it has permissions to mount under depends on
        // the kind of blessing: <idp>:u:<email>:... should have permissions under user/<email>/...
        // while <idp>:o:<appid>:<email> should ultimately go under apps/<appid>/users/<email>/...
        // but for now will be placed under tmp/debug/<something>.
        //
        // The hash of the public key of the principal is used to uniquely identify this particular
        // instance of the application.
        VPrincipal p = V.getPrincipal(ctx);
        String[] myNames = VSecurity.getBlessingNames(p, p.blessingStore().defaultBlessings());
        String uid = Base64.encodeToString(
                MessageDigest.getInstance("SHA-256").digest(p.publicKey().getEncoded()),
                BASE64_FLAGS);
        for (String n : myNames) {
            // Does this follow the "idp:u:<username>" path?
            String[] parts = n.split(io.v.v23.security.Constants.CHAIN_SEPARATOR);
            if (parts.length >=3 && parts[1].equals("u")) {
                return "users/" + parts[2] + "debug/" + uid;
            }
            if (parts.length >=4 && parts[1].equals("o")) {
                // TODO(ashankar,ribrdb): Replace this with whatever convention we end up with
                // when it comes to application blessings.
                return "tmp/debug/" + uid;
            }
        }
        return "";
    }
}
