// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.security;

import junit.framework.TestCase;

import org.joda.time.DateTime;

import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.verror.VException;
import io.v.x.jni.test.security.TestCaveatValidator;

import java.util.Arrays;

/**
 * Tests the various caveat implementations.
 */
public class CaveatTest extends TestCase {
    public void testMethodCaveat() throws VException {
        VContext context = V.init();
        VPrincipal p1 = VSecurity.newPrincipal();
        Blessings alice = p1.blessSelf("alice", VSecurity.newMethodCaveat("succeed"));
        p1.addToRoots(alice);
        {
            Call call = VSecurity.newCall(
                    new CallParams().withLocalPrincipal(p1).withRemoteBlessings(alice).withMethod("succeed"));
            String[] want = { "alice" };
            String[] got = VSecurity.getRemoteBlessingNames(context, call);
            if (!Arrays.equals(want, got)) {
                fail(String.format("Blessings differ, want %s, got %s",
                        Arrays.toString(want), Arrays.toString(got)));
            }
        }
        {
            Call call = VSecurity.newCall(
                    new CallParams().withLocalPrincipal(p1).withMethod("fail"));
            assertEquals(null, VSecurity.getRemoteBlessingNames(context, call));
        }
    }

    public void testExpiryCaveat() throws VException {
        VContext context = V.init();
        VPrincipal p1 = VSecurity.newPrincipal();
        Blessings alice = p1.blessSelf(
            "alice", VSecurity.newExpiryCaveat(DateTime.now().plusHours(1)));
        p1.addToRoots(alice);
        {
            Call call = VSecurity.newCall(new CallParams()
                    .withLocalPrincipal(p1)
                    .withRemoteBlessings(alice)
                    .withTimestamp(DateTime.now()));
            String[] want = { "alice" };
            String[] got = VSecurity.getRemoteBlessingNames(context, call);
            if (!Arrays.equals(want, got)) {
                fail(String.format("Blessings differ, want %s, got %s",
                        Arrays.toString(want), Arrays.toString(got)));
            }
        }
        {
            Call call = VSecurity.newCall(new CallParams()
                    .withLocalPrincipal(p1)
                    .withTimestamp(DateTime.now().plusHours(2)));
            assertEquals(null, VSecurity.getRemoteBlessingNames(context, call));
        }
    }

    public void testCustomCaveat() throws VException {
        VContext context = V.init();
        CaveatRegistry.register(io.v.x.jni.test.security.Constants.TEST_CAVEAT,
                new TestCaveatValidator());
        VPrincipal p1 = VSecurity.newPrincipal();
        Blessings alice = p1.blessSelf("alice",
                VSecurity.newCaveat(io.v.x.jni.test.security.Constants.TEST_CAVEAT, "succeed"));
        p1.addToRoots(alice);
        {
            Call call = VSecurity.newCall(new CallParams()
                    .withLocalPrincipal(p1)
                    .withRemoteBlessings(alice)
                    .withSuffix("succeed"));
            String[] want = { "alice" };
            String[] got = VSecurity.getRemoteBlessingNames(context, call);
            if (!Arrays.equals(want, got)) {
                fail(String.format("Blessings differ, want %s, got %s",
                        Arrays.toString(want), Arrays.toString(got)));
            }
        }
        {
            Call call = VSecurity.newCall(new CallParams()
                    .withLocalPrincipal(p1)
                    .withRemoteBlessings(alice)
                    .withSuffix("fail"));
            assertEquals(null, VSecurity.getRemoteBlessingNames(context, call));
        }
    }
}