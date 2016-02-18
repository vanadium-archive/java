// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.app.Activity;

import io.v.baku.toolkit.bind.SyncbaseBinding;
import io.v.baku.toolkit.bind.CollectionBinding;
import io.v.baku.toolkit.syncbase.BakuDb;
import io.v.baku.toolkit.syncbase.BakuSyncbase;
import io.v.baku.toolkit.syncbase.BakuTable;
import rx.subscriptions.CompositeSubscription;

/**
 * @see BakuActivityMixin
 */
public interface BakuActivityTrait<T extends Activity> extends AutoCloseable {
    VAndroidContextTrait<T> getVAndroidContextTrait();
    BakuSyncbase getSyncbase();
    BakuDb getSyncbaseDb();
    BakuTable getSyncbaseTable();
    CompositeSubscription getSubscriptions();
    String getSyncbaseTableName();
    void onSyncError(Throwable t);
    <U> SyncbaseBinding.Builder<U> binder();
    CollectionBinding.Builder collectionBinder();
    void close();
}
