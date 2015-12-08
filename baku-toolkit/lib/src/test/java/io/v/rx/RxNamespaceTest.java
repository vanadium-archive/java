// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.v.v23.rpc.MountStatus;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerStatus;
import java8.util.function.Predicate;
import rx.Observable;
import rx.Subscriber;
import rx.subjects.ReplaySubject;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RxNamespaceTest extends RxTestCase {
    /**
     * There is a hack in use in {@link RxNamespace} that compares any timestamp against an
     * arbitrary value after 1754, currently 1970. For this test, use 2000 as the time in the past.
     */
    private static final DateTime
            THE_PAST = new DateTime(2000, DateTimeConstants.JANUARY, 1, 0, 0),
            THE_DISTANT_PAST = new DateTime(1990, DateTimeConstants.JANUARY, 1, 0, 0);
    private static final long STATUS_POLLING_DELAY_MS = verificationDelay(
            RxMountState.DEFAULT_POLLING_INTERVAL);

    private class TestSubscriber extends Subscriber<MountEvent> {
        public boolean mayComplete;
        /**
         * This is a queue only as an implementation detail; we do not actually verify order here.
         */
        public final Collection<Predicate<MountEvent>> expectedEvents =
                new ConcurrentLinkedQueue<>();

        @Override
        public void onCompleted() {
            try {
                assertTrue("Unexpectedly unsubscribed", mayComplete);
            } catch (final Throwable t) {
                catchAsync(t);
            }
        }

        @Override
        public void onError(final Throwable t) {
            catchAsync(t);
        }

        @Override
        public void onNext(final MountEvent e) {
            for (final Iterator<Predicate<MountEvent>> tests = expectedEvents.iterator();
                 tests.hasNext();) {
                if (tests.next().test(e)) {
                    tests.remove();
                    return;
                }
            }
            //all-else
            fail("Unexpected mount event: " + e);
        }

        public void assertNoLeftoverExpectations() {
            //It would be nice to print these, but lambdas don't stringize into anything meaningful.
            assertTrue("Leftover expectations", expectedEvents.isEmpty());
        }
    }

    private static ServerStatus mockStatus(final MountStatus... ms) {
        return new ServerStatus(null, false, ms, null, null);
    }

    private static boolean isInitialMountAttemptStart(final MountEvent e) {
        return e.isMount() && !e.getServer().isPresent() && !e.getError().isPresent();
    }

    private final Server mServer = mock(Server.class);
    private final ReplaySubject<Server> mRxServer = ReplaySubject.create();
    private final TestSubscriber mSubscriber = new TestSubscriber();

    @Before
    public void setUp() {
        mRxServer.onNext(mServer);
    }

    @After
    public void tearDown() throws Exception {
        mSubscriber.assertNoLeftoverExpectations();
    }

    private void attachSubscriber(final Observable<MountEvent> mount) {
        mount.share().subscribe(mSubscriber);
    }

    @Test
    public void testAlreadyMounted() {
        when(mServer.getStatus()).thenReturn(mockStatus(new MountStatus("foo", "bar",
                THE_PAST, null, null, THE_DISTANT_PAST, null)));

        mSubscriber.expectedEvents.add(MountEvent::isSuccessfulMount);

        attachSubscriber(RxNamespace.mount(mRxServer, "foo"));
    }

    @Test
    public void testExistingOtherNames() {
        when(mServer.getStatus()).thenReturn(mockStatus(new MountStatus("baz", "bar",
                THE_PAST, null, null, THE_DISTANT_PAST, null)));

        mSubscriber.expectedEvents.add(RxNamespaceTest::isInitialMountAttemptStart);

        attachSubscriber(RxNamespace.mount(mRxServer, "foo"));
    }

    private void mountWithOldStatus() {
        when(mServer.getStatus()).thenReturn(mockStatus(new MountStatus("foo", "bar",
                THE_DISTANT_PAST, null, null, THE_PAST, null)));

        mSubscriber.expectedEvents.add(RxNamespaceTest::isInitialMountAttemptStart);

        attachSubscriber(RxNamespace.mount(mRxServer, "foo"));
    }

    @Test
    public void testMountWithOlderEvents() throws InterruptedException {
        mountWithOldStatus();

        Thread.sleep(STATUS_POLLING_DELAY_MS);
        assertNoAsyncErrors();
    }

    @Test
    public void testUnsubscribeOnUnmount() throws InterruptedException {
        mountWithOldStatus();
        Thread.sleep(BLOCKING_DELAY_MS);
        assertNoAsyncErrors();

        mSubscriber.mayComplete = true;
        when(mServer.getStatus()).thenReturn(mockStatus(new MountStatus("foo", "bar",
                THE_DISTANT_PAST, null, null, DateTime.now(), null)));
        Thread.sleep(STATUS_POLLING_DELAY_MS);
        assertTrue("Mount should unsubscribe after unmount", mSubscriber.isUnsubscribed());

        when(mServer.getStatus()).then(i -> {
            fail("Polling should stop after an unmount");
            return null;
        });
        Thread.sleep(STATUS_POLLING_DELAY_MS);
        assertNoAsyncErrors();
    }
}
