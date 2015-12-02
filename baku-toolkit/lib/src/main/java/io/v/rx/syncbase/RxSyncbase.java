// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import android.content.Context;

import io.v.baku.toolkit.VAndroidContextTrait;
import io.v.debug.SyncbaseClient;
import io.v.impl.google.naming.NamingUtil;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Server;
import io.v.v23.security.Blessings;
import io.v.v23.syncbase.SyncbaseService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import rx.Observable;

/**
 * Models a binding to a Syncbase Android service as an {@code Observable} of
 * {@link SyncbaseService}s. The binding will be asynchronously made and then potentially
 * periodically lost and regained, so modeling further operations as subscriptions works well.
 */
@Accessors(prefix = "m")
@AllArgsConstructor
public class RxSyncbase implements AutoCloseable {
    public static String syncgroupName(final String sgHost, final String sgSuffix) {
        return NamingUtil.join(sgHost, "%%sync", sgSuffix);
    }

    @Getter private final VContext mVContext;
    private final SyncbaseClient mClient;

    public Observable<Server> getRxServer() {
        return mClient.getRxServer();
    }

    public Observable<SyncbaseService> getRxClient() {
        return mClient.getRxClient();
    }

    public RxSyncbase(final Context androidContext, final VContext ctx,
                      final Observable<Blessings> rxBlessings) {
        mVContext = ctx;
        mClient = new SyncbaseClient(androidContext, rxBlessings);
    }

    public RxSyncbase(final VAndroidContextTrait trait) {
        this(trait.getAndroidContext(), trait.getVContext(),
                trait.getBlessingsProvider().getRxBlessings());
    }

    public void close() {
        mClient.close();
    }

    public RxApp rxApp(final String name) {
        return new RxApp(name, this);
    }
}
