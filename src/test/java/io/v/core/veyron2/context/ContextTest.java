package io.v.core.veyron2.context;

import android.test.AndroidTestCase;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import io.v.core.veyron2.android.VRuntime;

import java.util.concurrent.CountDownLatch;

/**
 * ContextTest tests the default Context implementation.
 */
public class ContextTest extends AndroidTestCase {
	public void testWithValue() {
		VRuntime.init(getContext(), null);
		final Context ctx = VRuntime.newContext();
		assertEquals(null, ctx.value("A"));
		final Context ctxA = ctx.withValue("A", 1);
		assertEquals(null, ctx.value("A"));
		assertEquals(1, ctxA.value("A"));
		assertEquals(null, ctx.value("B"));
		assertEquals(null, ctxA.value("B"));
		final Context ctxAB = ctxA.withValue("B", 2);
		assertEquals(null, ctx.value("A"));
		assertEquals(1, ctxA.value("A"));
		assertEquals(null, ctx.value("B"));
		assertEquals(null, ctxA.value("B"));
		assertEquals(1, ctxAB.value("A"));
		assertEquals(2, ctxAB.value("B"));
	}

	public void testWithCancel() {
		VRuntime.init(getContext(), null);
		final Context ctx = VRuntime.newContext();
		assertEquals(null, ctx.done());
		final CancelableContext ctxCancel = ctx.withCancel();
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
		VRuntime.init(getContext(), null);
		{
			final CancelableContext ctx =
					VRuntime.newContext().withDeadline(DateTime.now().plus(500));
			final CountDownLatch done = ctx.done();
			assertTrue(done != null);
			try {
				done.await();
			} catch (InterruptedException e) {
				fail("Interrupted");
			}
			assertEquals(0, done.getCount());
		}
		{
			final CancelableContext ctx =
					VRuntime.newContext().withDeadline(DateTime.now().plus(100000));
			final CountDownLatch done = ctx.done();
			assertTrue(done != null);
			ctx.cancel();
			try {
				done.await();
			} catch (InterruptedException e) {
				fail("Interrupted");
			}
			assertEquals(0, done.getCount());
		}
	}

	public void testWithTimeout() {
		VRuntime.init(getContext(), null);
		{
			final CancelableContext ctx =
					VRuntime.newContext().withTimeout(new Duration(500));
			final CountDownLatch done = ctx.done();
			assertTrue(done != null);
			try {
				done.await();
			} catch (InterruptedException e) {
				fail("Interrupted");
			}
			assertEquals(0, done.getCount());
		}
		{
			final CancelableContext ctx =
					VRuntime.newContext().withTimeout(new Duration(100000));
			final CountDownLatch done = ctx.done();
			assertTrue(done != null);
			ctx.cancel();
			try {
				done.await();
			} catch (InterruptedException e) {
				fail("Interrupted");
			}
			assertEquals(0, done.getCount());
		}
	}
}
