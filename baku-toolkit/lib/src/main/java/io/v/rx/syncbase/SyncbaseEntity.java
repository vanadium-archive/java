// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;
import io.v.v23.verror.ExistException;

abstract class SyncbaseEntity implements ExistenceAware, Creatable {
    public static SyncbaseEntity compose(final ExistenceAware fnExists, final Creatable fnCreate) {
        return new SyncbaseEntity() {
            @Override
            public ListenableFuture<Void> create(VContext vContext, Permissions permissions) {
                return fnCreate.create(vContext, permissions);
            }

            @Override
            public ListenableFuture<Boolean> exists(VContext vContext) {
                return fnExists.exists(vContext);
            }
        };
    }

    /**
     * Utility for Syncbase entities with lazy creation semantics. It would be great if this were
     * instead factored into a V23 interface and utility.
     */
    public ListenableFuture<Void> ensureExists(final VContext vContext,
                                               final Permissions permissions) {
        return Futures.transform(exists(vContext), (AsyncFunction<Boolean, Void>) (e -> e ?
                Futures.immediateFuture(null) :
                Futures.withFallback(create(vContext, permissions), t ->
                        t instanceof ExistException ?
                                Futures.immediateFuture(null) : Futures.immediateFailedFuture(t))
        ));
    }
}
