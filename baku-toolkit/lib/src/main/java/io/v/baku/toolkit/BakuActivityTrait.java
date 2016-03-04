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


public interface BakuActivityTrait<T extends Activity> extends AutoCloseable {
    VAndroidContextTrait<T> getVAndroidContextTrait();
    BakuSyncbase getSyncbase();
    BakuDb getSyncbaseDb();
    BakuTable getSyncbaseTable();
    CompositeSubscription getSubscriptions();
    String getSyncbaseTableName();
    void onSyncError(Throwable t);

    /**
     * Exposes a default scalar data binding builder for this Activity. The returned builder may be
     * freely customized; a new builder is returned for each call.
     */
    <U> SyncbaseBinding.Builder<U> binder();

    /**
     * Exposes a default collection data binding builder for this Activity. The returned builder may
     * be freely customized; a new builder is returned for each call.
     */
    CollectionBinding.Builder collectionBinder();
    void close();
}
