package io.v.v23.security;

import android.test.AndroidTestCase;

import org.joda.time.DateTime;

import io.v.v23.verror.VException;
import io.v.v23.android.V;
import io.v.x.jni.test.security.TestCaveatValidator;

import java.util.Arrays;

/**
 * Tests the various caveat implementations.
 */
public class CaveatTest extends AndroidTestCase {
    public void testMethodCaveat() {
        try {
            V.init(getContext(), null);
            final Principal p1 = Security.newPrincipal();
            final Blessings alice = p1.blessSelf("alice", Security.newMethodCaveat("succeed"));
            p1.addToRoots(alice);
            {
                final Call call = Security.newCall(
                        new CallParams().withLocalPrincipal(p1).withMethod("succeed"));
                final String[] want = { "alice" };
                final String[] got = alice.forCall(call);
                if (!Arrays.equals(want, got)) {
                    fail(String.format("Blessings differ, want %s, got %s",
                            Arrays.toString(want), Arrays.toString(got)));
                }
            }
            {
                final Call call = Security.newCall(
                        new CallParams().withLocalPrincipal(p1).withMethod("fail"));
                assertEquals(null, alice.forCall(call));
            }
        } catch (VException e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testExpiryCaveat() {
        try {
            V.init(getContext(), null);
            final Principal p1 = Security.newPrincipal();
            final Blessings alice = p1.blessSelf(
                "alice", Security.newExpiryCaveat(DateTime.now().plusHours(1)));
            p1.addToRoots(alice);
            {
                final Call call = Security.newCall(new CallParams()
                        .withLocalPrincipal(p1)
                        .withTimestamp(DateTime.now()));
                final String[] want = { "alice" };
                final String[] got = alice.forCall(call);
                if (!Arrays.equals(want, got)) {
                    fail(String.format("Blessings differ, want %s, got %s",
                            Arrays.toString(want), Arrays.toString(got)));
                }
            }
            {
                final Call call = Security.newCall(new CallParams()
                        .withLocalPrincipal(p1)
                        .withTimestamp(DateTime.now().plusHours(2)));
                assertEquals(null, alice.forCall(call));
            }
        } catch (VException e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    public void testCustomCaveat() {
        try {
            V.init(getContext(), null);
            CaveatRegistry.register(io.v.x.jni.test.security.Constants.TEST_CAVEAT,
                    new TestCaveatValidator());
            final Principal p1 = Security.newPrincipal();
            final Blessings alice = p1.blessSelf("alice",
                    Security.newCaveat(io.v.x.jni.test.security.Constants.TEST_CAVEAT, "succeed"));
            p1.addToRoots(alice);
            {
                final Call call = Security.newCall(new CallParams()
                        .withLocalPrincipal(p1)
                        .withSuffix("succeed"));
                final String[] want = { "alice" };
                final String[] got = alice.forCall(call);
                if (!Arrays.equals(want, got)) {
                    fail(String.format("Blessings differ, want %s, got %s",
                            Arrays.toString(want), Arrays.toString(got)));
                }
            }
            {
                final Call call = Security.newCall(new CallParams()
                        .withLocalPrincipal(p1)
                        .withSuffix("fail"));
                assertEquals(null, alice.forCall(call));
            }
        } catch (VException e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
}