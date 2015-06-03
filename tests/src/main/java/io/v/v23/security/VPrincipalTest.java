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

    public void testBlessingsByName() throws VException {
        V.init();
        final VPrincipal p1 = VSecurity.newPrincipal();
        final VPrincipal p2 = VSecurity.newPrincipal();
        final VPrincipal p3 = VSecurity.newPrincipal();
        final Blessings alice = p1.blessSelf("alice");
        final Blessings fake = p3.blessSelf("alice");

        final Blessings aliceWorkFriend = p1.bless(p2.publicKey(),
                alice, "work/friend", VSecurity.newUnconstrainedUseCaveat());
        final Blessings aliceGymFriend = p1.bless(p2.publicKey(),
                alice, "gym/friend", VSecurity.newUnconstrainedUseCaveat());
        final Blessings aliceWorkBoss = p1.bless(p2.publicKey(),
                alice, "work/boss", VSecurity.newUnconstrainedUseCaveat());
        final Blessings fakeFriend = p3.bless(p2.publicKey(),
                fake, "work/friend", VSecurity.newUnconstrainedUseCaveat());

        p2.addToRoots(alice);
        p2.blessingStore().set(aliceWorkFriend, new BlessingPattern("alice/work/friend"));
        p2.blessingStore().set(aliceGymFriend, new BlessingPattern("alice/gym/friend"));
        p2.blessingStore().set(aliceWorkBoss, new BlessingPattern("alice/work/boss"));
        p2.blessingStore().set(fakeFriend, new BlessingPattern("fake/work/friend"));

        final Map<Blessings[], BlessingPattern> testdata =
                ImmutableMap.<Blessings[], BlessingPattern>builder()
                .put(new Blessings[]{ aliceWorkFriend, aliceWorkBoss },
                        new BlessingPattern("alice/work"))
                .put(new Blessings[] { aliceWorkFriend },
                        new BlessingPattern("alice/work/friend"))
                .put(new Blessings[] { aliceGymFriend },
                        new BlessingPattern("alice/gym/friend"))
                .put(new Blessings[] { aliceWorkFriend, aliceGymFriend, aliceWorkBoss },
                        new BlessingPattern("alice"))
                .put(new Blessings[] { aliceWorkFriend, aliceGymFriend, aliceWorkBoss },
                        new BlessingPattern("..."))
                .put(new Blessings[] {}, new BlessingPattern("alice/school"))
                .build();
        for (Map.Entry<Blessings[], BlessingPattern> entry : testdata.entrySet()) {
            final Blessings[] want = entry.getKey();
            final Blessings[] got = p2.blessingsByName(entry.getValue());
            final Comparator<Blessings> comp = new Comparator<Blessings>() {
                @Override
                public int compare(Blessings b1, Blessings b2) {
                    final int h1 = b1.hashCode(),  h2 = b2.hashCode();
                    return h1 < h2 ? -1 : (h1 == h2 ? 0 : 1);
                }
            };
            Arrays.sort(want, comp);
            Arrays.sort(got, comp);
            if (!Arrays.equals(want, got)) {
                fail(String.format("Pattern: %s; got different blessings, want: %s, got: %s",
                        entry.getValue(), Arrays.toString(want), Arrays.toString(got)));
            }
        }
    }
}