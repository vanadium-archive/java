// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import com.google.common.collect.Lists;

import java.util.List;

import io.v.baku.toolkit.BakuActivityTrait;
import io.v.v23.services.syncbase.nosql.SyncgroupSpec;
import io.v.v23.services.syncbase.nosql.TableRow;
import io.v.v23.verror.ExistException;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;

import static net.javacrumbs.futureconverter.guavarx.FutureConverter.toObservable;

@Slf4j
public class UserCloudSyncgroup extends UserSyncgroup {
    public static UserCloudSyncgroup forActivity(final BakuActivityTrait t) {
        return builder().activity(t).buildCloud();
    }

    private Observable<Void> ensureSyncgroup(final String sgHost, final String sgName,
                                             final SyncgroupSpec spec) {
        // We need app/db/table to sync even on the cloud.
        // https://github.com/vanadium/issues/issues/857
        // Use idempotent APIs to allow failure recovery and avoid race conditions. Most of the
        // time, we'll just short-circuit and join the syncgroup from the get-go.
        final RxDb remoteDb = RxSyncbase.fromSyncbaseAt(mParams.getVContext(), sgHost)
                .rxApp(mParams.getDb().getRxApp().getName())
                .rxDb(mParams.getDb().getName());
        final List<String> tableNames =
                Lists.transform(mParams.getPrefixes(), TableRow::getTableName);

        return SgHostUtil.ensureSyncgroupHierarchies(remoteDb, tableNames)
                .switchMap(db -> toObservable(db.getSyncgroup(sgName)
                        .create(mParams.getVContext(), spec, mParams.getMemberInfo()))
                        .doOnCompleted(() ->
                                log.info("Created syncgroup " + sgName + " remotely"))
                        .onErrorResumeNext(t -> t instanceof ExistException ?
                                Observable.just(null) : Observable.error(t)));
    }

    private Observable<?> joinExistingSyncgroup(final String sgName) {
        return mParams.getDb().getObservable().switchMap(db -> toObservable(db.getSyncgroup(sgName)
                .join(mParams.getVContext(), mParams.getMemberInfo())))
                .doOnCompleted(() -> log.info("Joined syncgroup " + sgName));
    }

    @Override
    protected Observable<?> rxJoin(final String sgHost, final String sgName,
                                   final SyncgroupSpec spec) {
        // TODO(rosswang) try to join first
        return Observable.concat(
                ensureSyncgroup(sgHost, sgName, spec).ignoreElements(),
                joinExistingSyncgroup(sgName));
    }

    public UserCloudSyncgroup(final Parameters params) {
        super(params);
    }
}
