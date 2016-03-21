// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.Futures;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.v.baku.toolkit.blessings.BlessingsUtils;
import io.v.baku.toolkit.blessings.ClientUser;
import io.v.debug.SyncbaseAndroidClient;
import io.v.rx.RxPublisherState;
import io.v.rx.RxTestCase;
import io.v.v23.context.VContext;
import io.v.v23.rpc.PublisherEntry;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerStatus;
import io.v.v23.security.Blessings;
import io.v.v23.syncbase.SyncbaseApp;
import io.v.v23.syncbase.SyncbaseService;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.Syncgroup;
import java8.util.stream.RefStreams;
import rx.Observable;
import rx.Subscription;
import rx.subjects.ReplaySubject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@SuppressStaticInitializationFor("io.v.baku.toolkit.blessings.BlessingsUtils")
@PrepareForTest({BlessingsUtils.class, SgHostUtil.class})
public class UserPeerSyncgroupTest extends RxTestCase {
    private static final long STATUS_POLLING_DELAY_MS = verificationDelay(
            RxPublisherState.DEFAULT_POLLING_INTERVAL);

    private final VContext mVContext = mock(VContext.class);

    private final ReplaySubject<Blessings> mRxBlessings = ReplaySubject.create();
    private final ReplaySubject<Server> mRxServer = ReplaySubject.create();
    private final ReplaySubject<SyncbaseService> mRxClient = ReplaySubject.create();

    private final Server mServer = mock(Server.class);
    private final SyncbaseService mClient = mock(SyncbaseService.class);
    private final Database mDb = mock(Database.class);
    private final Syncgroup mSg = mock(Syncgroup.class);

    private RxAndroidSyncbase mSb;

    @Before
    public void setUp() throws Exception {
        final SyncbaseAndroidClient sbClient = mock(SyncbaseAndroidClient.class);
        final SyncbaseApp app = mock(SyncbaseApp.class);

        when(sbClient.getRxServer()).thenReturn(mRxServer);
        when(sbClient.getRxClient()).thenReturn(mRxClient);

        mRxServer.onNext(mServer);
        mRxClient.onNext(mClient);

        when(mClient.getApp("app")).thenReturn(app);
        when(app.getNoSqlDatabase(eq("db"), any())).thenReturn(mDb);

        when(app.exists(any())).thenReturn(Futures.immediateFuture(true));
        when(mDb.exists(any())).thenReturn(Futures.immediateFuture(true));

        when(mSg.join(any(), any())).thenReturn(Futures.immediateFuture(null));

        mSb = new RxAndroidSyncbase(null, sbClient);

        PowerMockito.spy(SgHostUtil.class);
    }

    private Subscription joinMockSyncgroup() throws Exception {
        when(mDb.getSyncgroup(RxSyncbase.syncgroupName("users/foo@bar.com/app/sghost", "sg")))
                .thenReturn(mSg);
        final Stopwatch t = Stopwatch.createStarted();

        final Subscription subscription = UserPeerSyncgroup.builder()
                .vContext(mVContext)
                .rxBlessings(mRxBlessings)
                .syncHostLevel(new UserAppSyncHost("app", "sghost", "sgmt"))
                .sgSuffix("sg")
                .db(mSb.rxApp("app").rxDb("db"))
                .onError((m, e) -> catchAsync(e))
                .buildPeer()
                .join();

        PowerMockito.spy(BlessingsUtils.class);
        PowerMockito.doReturn(RefStreams.of(new ClientUser("fooclient", "foo@bar.com")))
                .when(BlessingsUtils.class, "blessingsToClientUserStream", any(), any());
        PowerMockito.doReturn(null)
                .when(BlessingsUtils.class, "blessingsToAcl", any(), any());
        PowerMockito.doReturn(null)
                .when(BlessingsUtils.class, "homogeneousPermissions", any(), any());

        long elapsed = t.elapsed(TimeUnit.MILLISECONDS);
        if (elapsed > BLOCKING_DELAY_MS) {
            fail("UserPeerSyncgroup.join should not block; took " + elapsed + " ms (threshold " +
                    BLOCKING_DELAY_MS + " ms)");
        }
        return subscription;
    }

    @Test
    public void testJoinAlreadyMounted() throws Exception {
        PowerMockito.doReturn(Observable.just(true))
                .when(SgHostUtil.class, "isSyncbaseOnline", any(), any());
        joinMockSyncgroup();
        mRxBlessings.onNext(null);
        Thread.sleep(STATUS_POLLING_DELAY_MS +
                SgHostUtil.SYNCBASE_PING_TIMEOUT.getMillis());
        verify(mServer, never()).addName(any());
        verify(mServer, never()).getStatus();
        verify(mSg).join(any(), any());
    }

    @Test
    public void testJoinMountLifecycle() throws Exception {
        PowerMockito.doReturn(Observable.just(false))
                .when(SgHostUtil.class, "isSyncbaseOnline", any(), any());
        final Subscription subscription = joinMockSyncgroup();

        final AtomicInteger statusPolls = new AtomicInteger();
        final ServerStatus statusNone = mock(ServerStatus.class);
        when(statusNone.getPublisherStatus()).then(i -> {
            statusPolls.incrementAndGet();
            return new PublisherEntry[0];
        });
        when(mServer.getStatus()).thenReturn(statusNone);

        Thread.sleep(STATUS_POLLING_DELAY_MS);
        verify(mServer, never()).addName(any());
        assertEquals("Polling should not start until blessings are resolved", 0, statusPolls.get());

        mRxBlessings.onNext(null);
        //Verify 3 polls + initial to ensure polling loop is working.
        Thread.sleep(RxPublisherState.DEFAULT_POLLING_INTERVAL.getMillis() +
                SgHostUtil.SYNCBASE_PING_TIMEOUT.getMillis());
        verify(mSg, never()).join(any(), any());
        final String name = "users/foo@bar.com/app/sghost";
        verify(mServer).addName(name);

        final PublisherEntry mountedPublisherEntry = new PublisherEntry(name, "foo", new DateTime(),
                null, Duration.standardMinutes(5), new DateTime(0), null);
        final ServerStatus statusMounted = mock(ServerStatus.class);
        when(statusMounted.getPublisherStatus()).then(i -> {
            statusPolls.incrementAndGet();
            return new PublisherEntry[]{mountedPublisherEntry};
        });
        when(mServer.getStatus()).thenReturn(statusMounted);

        Thread.sleep(3 * RxPublisherState.DEFAULT_POLLING_INTERVAL.getMillis());
        final int nPolls = statusPolls.get();
        if (nPolls < 4) {
            fail("Polling should start and continue after blessings are resolved (" + nPolls +
                    "/4-5 expected polls)");
        }

        verify(mSg).join(any(), any());

        try {
            subscription.unsubscribe();
        } catch (final IllegalArgumentException expected) {
            // https://github.com/ReactiveX/RxJava/pull/3167
            // This should be fixed in the next version of RxJava.
        }
        statusPolls.set(0);
        Thread.sleep(STATUS_POLLING_DELAY_MS);
        assertEquals("Polling should stop after Syncgroup join is unsubscribed",
                0, statusPolls.get());
    }
}
