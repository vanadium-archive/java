// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.app.Activity;

import io.v.baku.toolkit.bind.SyncbaseBinding;
import io.v.rx.syncbase.RxDb;
import io.v.rx.syncbase.RxSyncbase;
import io.v.rx.syncbase.RxTable;
import rx.subscriptions.CompositeSubscription;

public interface BakuActivityTrait<T extends Activity> extends AutoCloseable {
    VAndroidContextTrait<T> getVAndroidContextTrait();
    RxSyncbase getSyncbase();
    RxDb getSyncbaseDb();
    RxTable getSyncbaseTable();
    CompositeSubscription getSubscriptions();
    String getSyncbaseTableName();
    void onSyncError(Throwable t);
    <U> SyncbaseBinding.Builder<U> binder();
    void close();
}
