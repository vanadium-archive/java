package io.v.core.veyron2.context;

import android.test.AndroidTestCase;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import io.v.core.veyron2.android.V;

import java.util.concurrent.CountDownLatch;

/**
 * ContextTest tests the default Context implementation.
 */
public class ContextTest extends AndroidTestCase {
	public void testWithValue() {
		final VContext ctx = V.init(getContext(), null);
		assertEquals(null, ctx.value("A"));
		final VContext ctxA = ctx.withValue("A", 1);
		assertEquals(null, ctx.value("A"));
		assertEquals(1, ctxA.value("A"));
		assertEquals(null, ctx.value("B"));
		assertEquals(null, ctxA.value("B"));
		final VContext ctxAB = ctxA.withValue("B", 2);
		assertEquals(null, ctx.value("A"));
		assertEquals(1, ctxA.value("A"));
		assertEquals(null, ctx.value("B"));
		assertEquals(null, ctxA.value("B"));
		assertEquals(1, ctxAB.value("A"));
		assertEquals(2, ctxAB.value("B"));
		final VContext ctxNull = ctxAB.withValue("C", null);
		assertEquals(null, ctxNull.value("C"));
		assertEquals(1, ctxAB.value("A"));
		assertEquals(2, ctxAB.value("B"));
	}

	public void testWithCancel() {
		final VContext ctx = V.init(getContext(), null);
		assertEquals(null, ctx.done());
		final CancelableVContext ctxCancel = ctx.withCancel();
		final CountDownLatch done = ctxCancel.done();
		assertTrue(done != null);
		assertEquals(done, ctxCancel.done());  // same value returned
		assertEquals(1, done.getCount());
		ctxCancel.cancel();
		try {
			done.await();
		} catch (InterruptedException e) {
			fail("Interrupted");
		}
		assertEquals(0, done.getCount());
	}

	public void testWithDeadline() {
		final VContext ctx = V.init(getContext(), null);
		{
			final CancelableVContext ctxD = ctx.withDeadline(DateTime.now().plus(500));
			final CountDownLatch done = ctxD.done();
			assertTrue(done != null);
			try {
				done.await();
			} catch (InterruptedException e) {
				fail("Interrupted");
			}
			assertEquals(0, done.getCount());
		}
		{
			final CancelableVContext ctxD = ctx.withDeadline(DateTime.now().plus(100000));
			final CountDownLatch done = ctxD.done();
			assertTrue(done != null);
			ctxD.cancel();
			try {
				done.await();
			} catch (InterruptedException e) {
				fail("Interrupted");
			}
			assertEquals(0, done.getCount());
		}
	}

	public void testWithTimeout() {
		final VContext ctx = V.init(getContext(), null);
		{
			final CancelableVContext ctxT = ctx.withTimeout(new Duration(500));
			final CountDownLatch done = ctxT.done();
			assertTrue(done != null);
			try {
				done.await();
			} catch (InterruptedException e) {
				fail("Interrupted");
			}
			assertEquals(0, done.getCount());
		}
		{
			final CancelableVContext ctxT = ctx.withTimeout(new Duration(100000));
			final CountDownLatch done = ctxT.done();
			assertTrue(done != null);
			ctxT.cancel();
			try {
				done.await();
			} catch (InterruptedException e) {
				fail("Interrupted");
			}
			assertEquals(0, done.getCount());
		}
	}
}
