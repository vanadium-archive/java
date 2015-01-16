package io.v.core.veyron2.security;

import android.test.AndroidTestCase;

import org.joda.time.DateTime;

import io.v.core.veyron2.VeyronException;
import io.v.core.veyron2.android.V;
import io.v.jni.test.security.TestCaveat;
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
				assertTrue(Arrays.equals(new String[]{ "alice" }, alice.forContext(ctx)));
			}
			{
				final VContext ctx = Security.newContext(
						new VContextParams().withLocalPrincipal(p1).withMethod("fail"));
				assertEquals(null, alice.forContext(ctx));
			}
		} catch (VeyronException e) {
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
				assertTrue(Arrays.equals(new String[]{ "alice" }, alice.forContext(ctx)));
			}
			{
				final VContext ctx = Security.newContext(new VContextParams()
						.withLocalPrincipal(p1)
						.withTimestamp(DateTime.now().plusHours(2)));
				assertEquals(null, alice.forContext(ctx));
			}
		} catch (VeyronException e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}

	public void testCustomCaveat() {
		try {
			V.init(getContext(), null);
			final Principal p1 = Security.newPrincipal();
			final Blessings alice = p1.blessSelf(
				"alice", Security.newCaveat(new TestCaveatValidator(new TestCaveat("succeed"))));
			p1.addToRoots(alice);
			{
				final VContext ctx = Security.newContext(new VContextParams()
						.withLocalPrincipal(p1)
						.withName("succeed"));
				assertTrue(Arrays.equals(new String[]{ "alice" }, alice.forContext(ctx)));
			}
			{
				final VContext ctx = Security.newContext(new VContextParams()
						.withLocalPrincipal(p1)
						.withName("fail"));
				assertEquals(null, alice.forContext(ctx));
			}
		} catch (VeyronException e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}
}