// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import io.v.android.v23.V;
import io.v.impl.google.naming.NamingUtil;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Server;
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

    public static RxSyncbase fromObservable(final VContext vContext,
                                            final Observable<SyncbaseService> sb) {
        final Observable<SyncbaseService> replay = sb.replay(1).autoConnect();
        return new RxSyncbase(vContext) {
            @Override
            public Observable<SyncbaseService> getRxClient() {
                return replay;
            }
        };
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

    public static RxSyncbase fromServer(final VContext vContext, final Server server) {
        return fromSyncbaseAt(vContext, "/" + server.getStatus().getEndpoints()[0]);
    }

    public static RxSyncbase fromServerContext(final VContext serverContext) {
        return fromServer(serverContext, V.getServer(serverContext));
    }

    @Getter
    private final VContext mVContext;

    public abstract Observable<SyncbaseService> getRxClient();

    public RxApp rxApp(final String name) {
        return new RxApp(name, this);
    }
}
