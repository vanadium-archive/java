// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import io.v.rx.VFn;
import io.v.v23.context.VContext;
import io.v.v23.syncbase.SyncbaseApp;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.verror.VException;
import lombok.Getter;
import lombok.experimental.Accessors;
import rx.Observable;

@Accessors(prefix = "m")
@Getter
public class RxDb extends RxEntity<Database, SyncbaseApp> {
    private final VContext mVContext;
    private final String mName;
    private final RxApp mRxApp;

    private final Observable<Database> mObservable;

    public RxDb(final String name, final RxApp rxApp) {
        mVContext = rxApp.getVContext();
        mName = name;
        mRxApp = rxApp;

        mObservable = rxApp.getObservable().map(VFn.unchecked(this::mapFrom));
    }

    @Override
    public Database mapFrom(final SyncbaseApp app) throws VException {
        final Database db = app.getNoSqlDatabase(mName, null);
        SyncbaseEntity.compose(db::exists, db::create).ensureExists(mVContext, null);
        return db;
    }

    public RxTable rxTable(final String name) {
        return new RxTable(name, this);
    }
}
