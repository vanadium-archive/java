// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase;

import com.google.common.util.concurrent.ListenableFuture;
import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.Id;
import io.v.v23.syncbase.util.AccessController;

import javax.annotation.CheckReturnValue;
import java.util.List;

/**
 * The interface for a Vanadium Syncbase service.
 */
public interface SyncbaseService extends AccessController {

    /**
     * Returns the database with the given user blessing and name.
     * <p>
     * This is a non-blocking method.
     *
     * @param databaseId Id of the database.
     * @param schema     database schema;  it can be {@code null} only if schema was never
     *                   set for the database in the first place
     * @return The handle {@link Database} to the database
     */
    Database getDatabase(Id databaseId, Schema schema);

    /**
     * Returns the database with the given relative name.
     * The user blessing is derived from the context.
     * <p>
     * This is a non-blocking method.
     *
     * @param ctx    Vanadium context
     * @param name   Name of the database
     * @param schema database schema;  it can be {@code null} only if schema was never
     *               set for the database in the first place
     * @return The handle {@link Database} to the Database
     */
    Database getDatabase(VContext ctx, String name, Schema schema);

    /**
     * Returns the full (i.e., object) name of this service.
     */
    String fullName();

    /**
     * Returns a {@link ListenableFuture} whose result is a list of all database ids.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param ctx Vanadium context
     */
    @CheckReturnValue
    ListenableFuture<List<Id>> listDatabases(VContext ctx);
}
