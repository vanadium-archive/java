// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import io.v.v23.context.VContext;
import io.v.v23.syncbase.SyncbaseApp;
import io.v.v23.syncbase.SyncbaseService;
import lombok.Getter;
import lombok.experimental.Accessors;
import rx.Observable;

import static net.javacrumbs.futureconverter.guavarx.FutureConverter.toObservable;

@Accessors(prefix = "m")
@Getter
public class RxApp extends RxEntity<SyncbaseApp, SyncbaseService> {
    private final VContext mVContext;
    private final String mName;
    private final RxSyncbase mRxSyncbase;

    private final Observable<SyncbaseApp> mObservable;

    public RxApp(final String name, final RxSyncbase rxSb) {
        mVContext = rxSb.getVContext();
        mName = name;
        mRxSyncbase = rxSb;

        mObservable = rxSb.getRxClient().flatMap(this::mapFrom);
    }

    @Override
    public Observable<SyncbaseApp> mapFrom(final SyncbaseService sb) {
        final SyncbaseApp app = sb.getApp(mName);
        return toObservable(SyncbaseEntity.compose(app::exists, app::create)
                .ensureExists(mVContext, null))
                .map(x -> app);
    }

    public RxDb rxDb(final String name) {
        return new RxDb(name, this);
    }
}
