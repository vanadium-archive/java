// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import com.google.common.util.concurrent.ListenableFuture;

import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.Schema;
import io.v.v23.syncbase.util.AccessController;
import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;

import java.util.List;

import javax.annotation.CheckReturnValue;

/**
 * A handle for an app running as part of a {@link SyncbaseService}.
 */
public interface SyncbaseApp extends AccessController {
    /**
     * Returns the relative name of this app.
     */
    String name();

    /**
     * Returns the full (i.e., object) name of this app.
     */
    String fullName();

    /**
     * Returns a new {@link ListenableFuture} whose result is {@code true} iff this app exists
     * and the user has sufficient permissions to access it.
     *
     * @param  ctx        Vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Boolean> exists(VContext ctx);

    /**
     * Returns a handle for a NoSQL database with the provided name.
     * <p>
     * Note that this database may not yet exist and can be created using the
     * {@link Database#create} method.
     * <p>
     * This is a non-blocking method.
     *
     * @param  relativeName name of the NoSQL database; must not contain slashes
     * @param  schema       database schema;  it can be {@code null} only if schema was never
     *                      set for the database in the first place
     * @return              a handle for a NoSQL database with the provided name
     */
    Database getNoSqlDatabase(String relativeName, Schema schema);

    /**
     * Returns a new {@link ListenableFuture} whose result is a list of all (relative) database
     * names.
     *
     * @param  ctx        Vanadium context
     */
    @CheckReturnValue
    ListenableFuture<List<String>> listDatabases(VContext ctx);

    /**
     * Creates the app.
     *
     * @param  ctx        Vanadium context
     * @param  perms      app permissions; if {@code null}, {@link SyncbaseService}'s
     *                    permissions are used
     */
    @CheckReturnValue
    ListenableFuture<Void> create(VContext ctx, Permissions perms);

    /**
     * Destroys the app.
     *
     * @param  ctx        Vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Void> destroy(VContext ctx);
}