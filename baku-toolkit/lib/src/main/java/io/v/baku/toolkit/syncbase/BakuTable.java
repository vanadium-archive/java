// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.syncbase;

import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;

import io.v.baku.toolkit.BakuActivityTrait;
import io.v.rx.syncbase.RangeWatchBatch;
import io.v.rx.syncbase.RxTable;
import io.v.rx.syncbase.SingleWatchEvent;
import io.v.v23.syncbase.nosql.PrefixRange;
import io.v.v23.syncbase.nosql.Table;
import lombok.Getter;
import lombok.experimental.Accessors;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;

@Accessors(prefix = "m")
@Getter
public class BakuTable extends RxTable {
    private final BakuActivityTrait<?> mBakuContext;

    public BakuTable(final RxTable t, final BakuActivityTrait bakuContext) {
        super(t);
        mBakuContext = bakuContext;
    }

    private <T> Observable<T> withActivityLifecycle(final Observable<T> o) {
        return o.takeUntil(x -> mBakuContext.getSubscriptions().isUnsubscribed());
    }

    @Override
    public <T> Observable<RangeWatchBatch<T>> watch(
            final PrefixRange prefix, final @Nullable Func1<String, Boolean> keyFilter,
            final Class<T> type) {
        return withActivityLifecycle(super.watch(prefix, keyFilter, type));
    }

    @Override
    public <T> Observable<SingleWatchEvent<T>> watch(
            final String key, final Class<T> type, final T defaultValue) {
        return withActivityLifecycle(super.watch(key, type, defaultValue));
    }

    /**
     * While the default implementation of {@link RxTable#exec(Func1)} provides an autoConnect
     * observable that requires an explicit subscription, this implementation auto-subscribes the
     * default Baku error handler and immediately begins the requested operation. An explicit
     * subscription unsubscribes the default error handler, and should include its own error
     * handler, which may still be the default error handler.
     */
    @Override
    public <T> Observable<T> exec(final Func1<Table, ListenableFuture<T>> op) {
        final Observable<T> exec = super.exec(op);
        final Subscription defaultSubscription = exec.subscribe(x -> {
        }, mBakuContext::onSyncError);
        // TODO(rosswang): This is a bit fragile, as it can result in the default onSyncError being
        // called in (edge edge) cases where the async completes before ... wait a second, does the
        // above have to observeOn the main thread?
        return withActivityLifecycle(exec.doOnSubscribe(defaultSubscription::unsubscribe));
    }
}
