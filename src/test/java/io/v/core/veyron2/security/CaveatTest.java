package io.v.core.veyron2.security;

import android.test.AndroidTestCase;

import org.joda.time.DateTime;

import io.v.core.veyron2.verror2.VException;
import io.v.core.veyron2.android.V;
import io.v.jni.test.security.TestCaveatValidator;

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
				final VContext ctx = Security.newContext(
						new VContextParams().withLocalPrincipal(p1).withMethod("succeed"));
				final String[] want = { "alice" };
				final String[] got = alice.forContext(ctx);
				if (!Arrays.equals(want, got)) {
					fail(String.format("Blessings differ, want %s, got %s",
							Arrays.toString(want), Arrays.toString(got)));
				}
			}
			{
				final VContext ctx = Security.newContext(
						new VContextParams().withLocalPrincipal(p1).withMethod("fail"));
				assertEquals(null, alice.forContext(ctx));
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
				final VContext ctx = Security.newContext(new VContextParams()
						.withLocalPrincipal(p1)
						.withTimestamp(DateTime.now()));
				final String[] want = { "alice" };
				final String[] got = alice.forContext(ctx);
				if (!Arrays.equals(want, got)) {
					fail(String.format("Blessings differ, want %s, got %s",
							Arrays.toString(want), Arrays.toString(got)));
				}
			}
			{
				final VContext ctx = Security.newContext(new VContextParams()
						.withLocalPrincipal(p1)
						.withTimestamp(DateTime.now().plusHours(2)));
				assertEquals(null, alice.forContext(ctx));
			}
		} catch (VException e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}

	public void testCustomCaveat() {
		try {
			V.init(getContext(), null);
			CaveatRegistry.register(io.v.jni.test.security.Constants.TEST_CAVEAT,
					new TestCaveatValidator());
			final Principal p1 = Security.newPrincipal();
			final Blessings alice = p1.blessSelf("alice",
					Security.newCaveat(io.v.jni.test.security.Constants.TEST_CAVEAT, "succeed"));
			p1.addToRoots(alice);
			{
				final VContext ctx = Security.newContext(new VContextParams()
						.withLocalPrincipal(p1)
						.withSuffix("succeed"));
				final String[] want = { "alice" };
				final String[] got = alice.forContext(ctx);
				if (!Arrays.equals(want, got)) {
					fail(String.format("Blessings differ, want %s, got %s",
							Arrays.toString(want), Arrays.toString(got)));
				}
			}
			{
				final VContext ctx = Security.newContext(new VContextParams()
						.withLocalPrincipal(p1)
						.withSuffix("fail"));
				assertEquals(null, alice.forContext(ctx));
			}
		} catch (VException e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}
}