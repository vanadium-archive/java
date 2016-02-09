// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import io.v.impl.google.naming.NamingUtil;
import io.v.v23.context.VContext;
import io.v.v23.syncbase.Syncbase;
import io.v.v23.syncbase.SyncbaseService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;
import rx.Observable;

@Accessors(prefix = "m")
@AllArgsConstructor
public abstract class RxSyncbase {
    public static String syncgroupName(final String sgHost, final String sgSuffix) {
        return NamingUtil.join(sgHost, "%%sync", sgSuffix);
    }

    /**
     * The {@link RxSyncbase#getRxClient()} produced by this factory method will produce exactly
     * one {@link SyncbaseService}.
     */
    public static RxSyncbase fromSyncbaseService(final VContext vContext,
                                                 final SyncbaseService sb) {
        return new RxSyncbase(vContext) {
            @Override
            public Observable<SyncbaseService> getRxClient() {
                return Observable.just(sb);
            }
        };
    }

    /**
     * @see #fromSyncbaseService(VContext, SyncbaseService)
     */
    public static RxSyncbase fromSyncbaseAt(final VContext vContext, final String name) {
        return fromSyncbaseService(vContext, Syncbase.newService(name));
    }

    @Getter
    private final VContext mVContext;

    public abstract Observable<SyncbaseService> getRxClient();

    public RxApp rxApp(final String name) {
        return new RxApp(name, this);
    }
}
