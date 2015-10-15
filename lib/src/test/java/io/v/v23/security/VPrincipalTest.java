// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.security;

import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

import io.v.v23.V;
import io.v.v23.verror.VException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;


/**
 * Tests the default {@code VPrincipal} implementation.
 */
public class VPrincipalTest extends TestCase {
    public void testBlessingsInfo() throws VException {
        V.init();
        final VPrincipal p1 = VSecurity.newPrincipal();
        final VPrincipal p2 = VSecurity.newPrincipal();
        final Blessings alice = p1.blessSelf("alice");
        p2.addToRoots(alice);

        final Blessings aliceWorkFriend = p1.bless(p2.publicKey(),
                alice, "work/friend", VSecurity.newUnconstrainedUseCaveat());
        final Blessings aliceGymFriend = p1.bless(p2.publicKey(),
                alice, "gym/friend", VSecurity.newUnconstrainedUseCaveat());
        final Blessings aliceAllFriends = VSecurity.unionOfBlessings(
            aliceWorkFriend, aliceGymFriend);
        assertInfoMapsEqual(ImmutableMap.<String, Caveat[]>builder()
                .put("alice/work/friend", new Caveat[]{VSecurity.newUnconstrainedUseCaveat()})
                .build(), p2.blessingsInfo(aliceWorkFriend));
        assertInfoMapsEqual(ImmutableMap.<String, Caveat[]>builder()
                .put("alice/gym/friend", new Caveat[]{VSecurity.newUnconstrainedUseCaveat()})
                .build(), p2.blessingsInfo(aliceGymFriend));
        assertInfoMapsEqual(ImmutableMap.<String, Caveat[]>builder()
                .put("alice/work/friend", new Caveat[]{VSecurity.newUnconstrainedUseCaveat()})
                .put("alice/gym/friend", new Caveat[]{VSecurity.newUnconstrainedUseCaveat()})
                .build(), p2.blessingsInfo(aliceAllFriends));
    }

    private void assertInfoMapsEqual(Map<String, Caveat[]> want, Map<String, Caveat[]> got) {
        assertEquals(want.size(), got.size());
        final String[] keysWant = want.keySet().toArray(new String[0]);
        final String[] keysGot = got.keySet().toArray(new String[0]);
        Arrays.sort(keysWant);
        Arrays.sort(keysGot);
        if (!Arrays.equals(keysWant, keysGot)) {
            fail(String.format("Blessings mismatch, want %s, got %s", Arrays.toString(keysWant),
                    Arrays.toString(keysGot)));
        }
        for (String key : keysWant) {
            final Caveat[] caveatsWant = want.get(key);
            final Caveat[] caveatsGot = got.get(key);
            if (!Arrays.equals(caveatsWant, caveatsGot)) {
                fail(String.format("Blessing %s caveat mismatch: want %s, got %s", key,
                        Arrays.toString(caveatsWant), Arrays.toString(caveatsGot)));
            }
        }
    }
}
