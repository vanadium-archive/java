// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.security;

import junit.framework.TestCase;

import io.v.v23.V;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

import java.util.Arrays;

/**
 * Tests the default Blessings implementation.
 */
public class BlessingsTest extends TestCase {
    public void testPublicKey() throws VException {
        V.init();
        VPrincipal p1 = VSecurity.newPrincipal();
        VPrincipal p2 = VSecurity.newPrincipal();
        Blessings alice = p1.blessSelf("alice");
        assertTrue(Arrays.equals(p1.publicKey().getEncoded(), alice.publicKey().getEncoded()));
        p2.addToRoots(alice);

        Blessings aliceWorkFriend = p1.bless(p2.publicKey(),
                alice, "work/friend", VSecurity.newUnconstrainedUseCaveat());
        if (!Arrays.equals(
                aliceWorkFriend.publicKey().getEncoded(), p2.publicKey().getEncoded())) {
            fail(String.format("Expected public key: %s, got %s",
                    aliceWorkFriend.publicKey().getEncoded(), p2.publicKey().getEncoded()));
        }
    }

    public void testVomEncodeDecode() throws VException {
        V.init();
        VPrincipal p = VSecurity.newPrincipal();
        Blessings alice = p.blessSelf("alice");
        byte[] data = VomUtil.encode(alice, Blessings.class);
        Blessings aliceCopy = (Blessings) VomUtil.decode(data, Blessings.class);
        if (!alice.equals(aliceCopy)) {
            fail(String.format("Blessings don't match, want %s, got %s", alice, aliceCopy));
        }
    }
}
