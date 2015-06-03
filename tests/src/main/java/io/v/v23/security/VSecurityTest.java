// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.security;

import junit.framework.TestCase;

import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.verror.VException;

import java.util.Arrays;

/**
 * Tests for the {@link VSecurity} utility methods.
 */
public class VSecurityTest extends TestCase {
    public void testGetRemoteBlessingNames() throws Exception {
        VContext context = V.init();
        VPrincipal p1 = VSecurity.newPrincipal();
        VPrincipal p2 = VSecurity.newPrincipal();
        Blessings alice = p1.blessSelf("alice");
        p2.addToRoots(alice);

        Blessings aliceWorkFriend = p1.bless(p2.publicKey(),
                alice, "work/friend", VSecurity.newUnconstrainedUseCaveat());
        Call call = VSecurity.newCall(
                new CallParams().withRemoteBlessings(aliceWorkFriend).withLocalPrincipal(p2));
        String[] blessings = VSecurity.getRemoteBlessingNames(context, call);
        if (!Arrays.equals(new String[]{ "alice/work/friend" }, blessings)) {
            fail(String.format("Expected blessings [\"alice/work/friend\"], got %s",
                    Arrays.toString(blessings)));
        }
    }

    public void testGetLocalBlessingNames() throws Exception {
        VContext context = V.init();
        VPrincipal p1 = VSecurity.newPrincipal();
        VPrincipal p2 = VSecurity.newPrincipal();
        Blessings alice = p1.blessSelf("alice");
        p2.addToRoots(alice);

        Blessings aliceWorkFriend = p1.bless(p2.publicKey(),
                alice, "work/friend", VSecurity.newUnconstrainedUseCaveat());
        Call call = VSecurity.newCall(
                new CallParams().withLocalBlessings(aliceWorkFriend).withLocalPrincipal(p2));
        String[] blessings = VSecurity.getLocalBlessingNames(context, call);
        if (!Arrays.equals(new String[]{ "alice/work/friend" }, blessings)) {
            fail(String.format("Expected blessings [\"alice/work/friend\"], got %s",
                    Arrays.toString(blessings)));
        }
    }

    public void testSigning() throws Exception {
        VSigner signer = VSecurity.newInMemorySigner();
        byte[] purpose = (new String("test")).getBytes();
        byte[] msg = (new String("this is a signing test message")).getBytes();
        VSignature signature = signer.sign(purpose, msg);
        try {
            VSecurity.verifySignature(signature, signer.publicKey(), msg);
        } catch (VException e) {
            fail(String.format("Couldn't verify signature: %s", e.getMessage()));
        }
    }
}
