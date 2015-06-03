// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.context;

import com.google.common.util.concurrent.Uninterruptibles;

import junit.framework.TestCase;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import io.v.v23.V;

import java.util.concurrent.CountDownLatch;

/**
 * ContextTest tests the default Context implementation.
 */
public class ContextTest extends TestCase {
    public void testWithValue() {
        VContext ctx = V.init();
        assertEquals(null, ctx.value("A"));
        VContext ctxA = ctx.withValue("A", 1);
        assertEquals(null, ctx.value("A"));
        assertEquals(1, ctxA.value("A"));
        assertEquals(null, ctx.value("B"));
        assertEquals(null, ctxA.value("B"));
        VContext ctxAB = ctxA.withValue("B", 2);
        assertEquals(null, ctx.value("A"));
        assertEquals(1, ctxA.value("A"));
        assertEquals(null, ctx.value("B"));
        assertEquals(null, ctxA.value("B"));
        assertEquals(1, ctxAB.value("A"));
        assertEquals(2, ctxAB.value("B"));
        VContext ctxNull = ctxAB.withValue("C", null);
        assertEquals(null, ctxNull.value("C"));
        assertEquals(1, ctxAB.value("A"));
        assertEquals(2, ctxAB.value("B"));
    }

    public void testWithCancel() {
        VContext ctx = V.init();
        CancelableVContext ctxCancel = ctx.withCancel();
        CountDownLatch done = ctxCancel.done();
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
        VContext ctx = V.init();
        {
            CancelableVContext ctxD = ctx.withDeadline(DateTime.now().plus(500));
            CountDownLatch done = ctxD.done();
            assertTrue(done != null);
            Uninterruptibles.awaitUninterruptibly(done);
            assertEquals(0, done.getCount());
        }
        {
            CancelableVContext ctxD = ctx.withDeadline(DateTime.now().plus(100000));
            CountDownLatch done = ctxD.done();
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
        VContext ctx = V.init();
        {
            CancelableVContext ctxT = ctx.withTimeout(new Duration(500));
            CountDownLatch done = ctxT.done();
            assertTrue(done != null);
            try {
                done.await();
            } catch (InterruptedException e) {
                fail("Interrupted");
            }
            assertEquals(0, done.getCount());
        }
        {
            CancelableVContext ctxT = ctx.withTimeout(new Duration(100000));
            CountDownLatch done = ctxT.done();
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
