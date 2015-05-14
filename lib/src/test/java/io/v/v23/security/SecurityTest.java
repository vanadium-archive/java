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
 * Tests for the {@link Security} utility methods.
 */
public class SecurityTest extends TestCase {
    public void testGetRemoteBlessingNames() throws Exception {
        final VContext context = V.init();
        final Principal p1 = Security.newPrincipal();
        final Principal p2 = Security.newPrincipal();
        final Blessings alice = p1.blessSelf("alice");
        p2.addToRoots(alice);

        final Blessings aliceWorkFriend = p1.bless(p2.publicKey(),
                alice, "work/friend", Security.newUnconstrainedUseCaveat());
        final Call call = Security.newCall(
                new CallParams().withRemoteBlessings(aliceWorkFriend).withLocalPrincipal(p2));
        final String[] blessings = Security.getRemoteBlessingNames(context, call);
        if (!Arrays.equals(new String[]{ "alice/work/friend" }, blessings)) {
            fail(String.format("Expected blessings [\"alice/work/friend\"], got %s",
                    Arrays.toString(blessings)));
        }
    }

    public void testGetLocalBlessingNames() throws Exception {
        final VContext context = V.init();
        final Principal p1 = Security.newPrincipal();
        final Principal p2 = Security.newPrincipal();
        final Blessings alice = p1.blessSelf("alice");
        p2.addToRoots(alice);

        final Blessings aliceWorkFriend = p1.bless(p2.publicKey(),
                alice, "work/friend", Security.newUnconstrainedUseCaveat());
        final Call call = Security.newCall(
                new CallParams().withLocalBlessings(aliceWorkFriend).withLocalPrincipal(p2));
        final String[] blessings = Security.getLocalBlessingNames(context, call);
        if (!Arrays.equals(new String[]{ "alice/work/friend" }, blessings)) {
            fail(String.format("Expected blessings [\"alice/work/friend\"], got %s",
                    Arrays.toString(blessings)));
        }
    }
}
