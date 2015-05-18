// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.security;

import com.google.common.collect.ImmutableMap;

import junit.framework.TestCase;

import io.v.v23.V;
import io.v.v23.verror.VException;

import java.util.Map;

/**
 * Tests the default {@code BlessingStore} implementation.
 */
public class BlessingStoreTest extends TestCase {
    public void testSet() throws VException {
        V.init();
        Principal principal = Security.newPrincipal();
        BlessingStore store = principal.blessingStore();
        Blessings blessingA = newBlessing(principal, "root", "A");
        Blessings blessingB = newBlessing(principal, "root", "B");
        Blessings blessingOther = Security.newPrincipal().blessSelf("other");
        Map<BlessingPattern, Blessings> want =
                ImmutableMap.<BlessingPattern, Blessings>builder()
                .put(new BlessingPattern("..."), blessingA)
                .put(new BlessingPattern("foo"), blessingA)
                .put(new BlessingPattern("bar"), blessingB)
                .build();
        Map<BlessingPattern, Blessings> errors =
                ImmutableMap.<BlessingPattern, Blessings>builder()
                .put(new BlessingPattern("..."), blessingOther)
                .put(new BlessingPattern(""), blessingA)
                .put(new BlessingPattern("foo..."), blessingA)
                .put(new BlessingPattern("...foo"), blessingB)
                .put(new BlessingPattern("foo/.../bar"), blessingB)
                .build();

        for (Map.Entry<BlessingPattern, Blessings> entry : want.entrySet()) {
            store.set(entry.getValue(), entry.getKey());
        }
        for (Map.Entry<BlessingPattern, Blessings> entry : errors.entrySet()) {
            try {
                store.set(entry.getValue(), entry.getKey());
                fail("Expected error for pattern: " + entry.getValue());
            } catch (VException e) {
                // OK
            }
        }
        Map<BlessingPattern, Blessings> actual = store.peerBlessings();
        assertEquals(want, actual);
    }

    public void testSetDefault() throws VException {
        V.init();
        Principal principal = Security.newPrincipal();
        BlessingStore store = principal.blessingStore();
        Blessings blessingA = newBlessing(principal, "root", "A");
        Blessings blessingB = newBlessing(principal, "root", "B");
        assertTrue(store.defaultBlessings().isEmpty());
        store.setDefaultBlessings(blessingA);
        assertEquals(blessingA, store.defaultBlessings());
        store.setDefaultBlessings(blessingB);
        assertEquals(blessingB, store.defaultBlessings());
    }

    public void testForPeer() throws VException {
        V.init();
        Principal principal = Security.newPrincipal();
        BlessingStore store = principal.blessingStore();
        Blessings blessingFoo = newBlessing(principal, "foo", "A");
        Blessings blessingBar = newBlessing(principal, "bar", "B");
        Blessings blessingAll = newBlessing(principal, "all", "C");
        store.set(blessingAll, new BlessingPattern("..."));
        store.set(blessingFoo, new BlessingPattern("foo"));
        store.set(blessingBar, new BlessingPattern("bar/$"));

        Map<String[], Blessings> testdata =
               ImmutableMap.<String[], Blessings>builder()
               .put(new String[] {}, blessingAll)
               .put(new String[]{ "baz" }, blessingAll)
               .put(new String[]{ "foo" }, Security.unionOfBlessings(blessingAll, blessingFoo))
               .put(new String[]{ "bar" }, Security.unionOfBlessings(blessingAll, blessingBar))
               .put(new String[]{ "foo/foo" },
                       Security.unionOfBlessings(blessingAll, blessingFoo))
               .put(new String[] { "bar/baz" }, blessingAll)
               .put(new String[] { "foo/foo/bar" },
                       Security.unionOfBlessings(blessingAll, blessingFoo))
               .put(new String[] { "bar/foo", "foo" },
                       Security.unionOfBlessings(blessingAll, blessingFoo))
               .put(new String[] { "bar", "foo" },
                       Security.unionOfBlessings(blessingAll, blessingFoo, blessingBar))
               .build();
        for (Map.Entry<String[], Blessings> entry : testdata.entrySet()) {
            store.forPeer(entry.getKey());
            assertEquals(entry.getValue(), store.forPeer(entry.getKey()));
        }
    }

    private static Blessings newBlessing(Principal blessee, String root, String extension)
            throws VException {
        Principal blesser = Security.newPrincipal();
        return blesser.bless(blessee.publicKey(), blesser.blessSelf(root), extension,
                Security.newUnconstrainedUseCaveat());
    }
}