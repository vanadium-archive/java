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
        Principal p1 = Security.newPrincipal();
        Blessings alice = p1.blessSelf("alice", Security.newMethodCaveat("succeed"));
        p1.addToRoots(alice);
        {
            Call call = Security.newCall(
                    new CallParams().withLocalPrincipal(p1).withRemoteBlessings(alice).withMethod("succeed"));
            String[] want = { "alice" };
            String[] got = Security.getRemoteBlessingNames(context, call);
            if (!Arrays.equals(want, got)) {
                fail(String.format("Blessings differ, want %s, got %s",
                        Arrays.toString(want), Arrays.toString(got)));
            }
        }
        {
            Call call = Security.newCall(
                    new CallParams().withLocalPrincipal(p1).withMethod("fail"));
            assertEquals(null, Security.getRemoteBlessingNames(context, call));
        }
    }

    public void testExpiryCaveat() throws VException {
        VContext context = V.init();
        Principal p1 = Security.newPrincipal();
        Blessings alice = p1.blessSelf(
            "alice", Security.newExpiryCaveat(DateTime.now().plusHours(1)));
        p1.addToRoots(alice);
        {
            Call call = Security.newCall(new CallParams()
                    .withLocalPrincipal(p1)
                    .withRemoteBlessings(alice)
                    .withTimestamp(DateTime.now()));
            String[] want = { "alice" };
            String[] got = Security.getRemoteBlessingNames(context, call);
            if (!Arrays.equals(want, got)) {
                fail(String.format("Blessings differ, want %s, got %s",
                        Arrays.toString(want), Arrays.toString(got)));
            }
        }
        {
            Call call = Security.newCall(new CallParams()
                    .withLocalPrincipal(p1)
                    .withTimestamp(DateTime.now().plusHours(2)));
            assertEquals(null, Security.getRemoteBlessingNames(context, call));
        }
    }

    public void testCustomCaveat() throws VException {
        VContext context = V.init();
        CaveatRegistry.register(io.v.x.jni.test.security.Constants.TEST_CAVEAT,
                new TestCaveatValidator());
        Principal p1 = Security.newPrincipal();
        Blessings alice = p1.blessSelf("alice",
                Security.newCaveat(io.v.x.jni.test.security.Constants.TEST_CAVEAT, "succeed"));
        p1.addToRoots(alice);
        {
            Call call = Security.newCall(new CallParams()
                    .withLocalPrincipal(p1)
                    .withRemoteBlessings(alice)
                    .withSuffix("succeed"));
            String[] want = { "alice" };
            String[] got = Security.getRemoteBlessingNames(context, call);
            if (!Arrays.equals(want, got)) {
                fail(String.format("Blessings differ, want %s, got %s",
                        Arrays.toString(want), Arrays.toString(got)));
            }
        }
        {
            Call call = Security.newCall(new CallParams()
                    .withLocalPrincipal(p1)
                    .withRemoteBlessings(alice)
                    .withSuffix("fail"));
            assertEquals(null, Security.getRemoteBlessingNames(context, call));
        }
    }
}