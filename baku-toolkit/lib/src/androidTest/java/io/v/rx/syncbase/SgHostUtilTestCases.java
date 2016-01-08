// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import android.content.Context;

import org.joda.time.Duration;

import java.util.concurrent.TimeUnit;

import io.v.debug.SyncbaseClient;
import io.v.v23.context.VContext;
import lombok.RequiredArgsConstructor;
import rx.Observable;
import rx.observables.BlockingObservable;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RequiredArgsConstructor
public class SgHostUtilTestCases {
    private static final Duration TIMEOUT = Duration.standardSeconds(20);

    private static <T> BlockingObservable<T> block(final Observable<T> source) {
        return source.timeout(TIMEOUT.getMillis(), TimeUnit.MILLISECONDS)
                .toBlocking();
    }

    private final Context mContext;
    private final VContext mVContext;

    public void testPing() {
        assertFalse(block(SgHostUtil.isSyncbaseOnline(mVContext,
                "users/jenkins.veyron@gmail.com/integ/fakesghost")).single());
    }

    public void testEnsureSgHost() {
        final String name = "users/jenkins.veyron@gmail.com/integ/ensuredsghost";
        try (final SyncbaseClient sb = new SyncbaseClient(mContext, null)) {
            block(SgHostUtil.ensureSyncgroupHost(mVContext, sb.getRxServer(), name)).first();
            assertTrue(block(SgHostUtil.isSyncbaseOnline(mVContext, name)).first());
        }
    }

    // TODO(rosswang): Figure out wtf is going on with blessings to actually make this work.
    /*public void testGlobalUserSyncgroup() {
        final Observable<Blessings> blessings =
                Observable.just(V.getPrincipal(mVContext).blessingStore().forPeer("..."));
        try (final SyncbaseClient sb = new SyncbaseClient(mContext, blessings)) {
            final RxSyncbase rsb = new RxSyncbase(mVContext, sb);
            block(GlobalUserSyncgroup.builder()
                    .syncbase(rsb)
                    .db(rsb.rxApp("app").rxDb("db"))
                    .sgSuffix("test")
                    .syncHostLevel(new UserAppSyncHost("integ"))
                    .rxBlessings(blessings)
                    .build()
                    .rxJoin()).first();
        }
    }*/
}
