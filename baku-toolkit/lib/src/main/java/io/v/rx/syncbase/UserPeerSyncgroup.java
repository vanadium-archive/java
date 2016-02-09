// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import io.v.baku.toolkit.BakuActivityTrait;
import io.v.v23.services.syncbase.nosql.SyncgroupJoinFailedException;
import io.v.v23.services.syncbase.nosql.SyncgroupSpec;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.Syncgroup;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;

import static net.javacrumbs.futureconverter.guavarx.FutureConverter.toObservable;

/**
 * This syncgroup strategy is a bit of a hack and its future is uncertain.
 */
@Accessors(prefix = "m")
@Slf4j
public class UserPeerSyncgroup extends UserSyncgroup {
    public static UserPeerSyncgroup forActivity(final BakuActivityTrait t) {
        return builder().activity(t).buildPeer();
    }

    private Observable<SyncgroupSpec> createOrJoinSyncgroup(final Database db, final String sgName,
                                                            final SyncgroupSpec spec) {
        final Syncgroup sg = db.getSyncgroup(sgName);
        return Observable.defer(() ->
                toObservable(sg.join(mParams.getVContext(), mParams.getMemberInfo())))
                .doOnCompleted(() -> log.info("Joined syncgroup " + sgName))
                .onErrorResumeNext(t -> t instanceof SyncgroupJoinFailedException ?
                        toObservable(
                                sg.create(mParams.getVContext(), spec, mParams.getMemberInfo()))
                                .doOnCompleted(() -> log.info("Created syncgroup " + sgName))
                                .map(x -> spec) :
                        Observable.error(t));
    }

    @Override
    protected Observable<?> rxJoin(final String sgHost, final String sgName,
                                   final SyncgroupSpec spec) {
        final RxAndroidSyncbase sb = (RxAndroidSyncbase) mParams.getDb().getRxApp().getRxSyncbase();
        final Observable<Object> mount = SgHostUtil.ensureSyncgroupHost(
                mParams.getVContext(), sb.getRxServer(), sgHost).share();

        return mParams.getDb().getObservable()
                .switchMap(db -> Observable.merge(mount.first().ignoreElements()
                        .concatWith(createOrJoinSyncgroup(db, sgName, spec)), mount));
    }

    public UserPeerSyncgroup(final Parameters params) {
        super(params);
        if (!(params.getDb().getRxApp().getRxSyncbase() instanceof RxAndroidSyncbase)) {
            throw new IllegalArgumentException("UserPeerSyncgroup must be constructed with a " +
                    "local Syncbase server (RxAndroidSyncbase).");
        }
    }
}
