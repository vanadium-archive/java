// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.robotninjas.concurrent.FluentFuture;
import org.robotninjas.concurrent.FluentFutures;

import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;
import io.v.v23.syncbase.SyncbaseApp;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.Table;
import io.v.v23.verror.ExistException;

public abstract class SyncbaseEntity implements ExistenceAware, Creatable {
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

    public static SyncbaseEntity forApp(final SyncbaseApp app) {
        return compose(app::exists, app::create);
    }

    public static SyncbaseEntity forDb(final Database db) {
        return compose(db::exists, db::create);
    }

    public static SyncbaseEntity forTable(final Table table) {
        return compose(table::exists, table::create);
    }

    private SyncbaseEntity(){}

    /**
     * Utility for Syncbase entities with lazy creation semantics. It would be great if this were
     * instead factored into a V23 interface and utility.
     *
     * @return a future that completes with {@code true} if this call created the entity,
     * {@code false} if the entity already existed, or fails if an unexpected error occurred.
     */
    public FluentFuture<Boolean> ensureExists(final VContext vContext,
                                              final Permissions permissions) {
        return FluentFutures.from(exists(vContext))
                .transform((AsyncFunction<Boolean, Boolean>) (e -> e ?
                        Futures.immediateFuture(false) :
                        FluentFutures.from(create(vContext, permissions))
                                .transform((Function<Void, Boolean>) (x -> true))
                                .withFallback(t -> t instanceof ExistException ?
                                        Futures.<Boolean>immediateFuture(false) :
                                        Futures.<Boolean>immediateFailedFuture(t))
                ));
    }

    /**
     * Equivalent to calling {@link #ensureExists(VContext, Permissions)} with null permissions,
     * inheriting permissions from the hierarchy.
     */
    public FluentFuture<Boolean> ensureExists(final VContext vContext) {
        return ensureExists(vContext, null);
    }
}
