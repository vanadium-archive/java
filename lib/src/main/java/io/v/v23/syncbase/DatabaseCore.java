// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase;

import com.google.common.util.concurrent.ListenableFuture;
import io.v.v23.InputChannel;
import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.Id;
import io.v.v23.services.watch.ResumeMarker;
import io.v.v23.vdl.VdlAny;

import javax.annotation.CheckReturnValue;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Base interface for {@link Database} and {@link BatchDatabase}, allowing clients to pass the
 * handle to helper methods that are batch-agnostic.
 */
public interface DatabaseCore {
    /**
     * Returns the id of the database.
     */
    Id id();

    /**
     * Returns the full (i.e., object) name of the database.
     */
    String fullName();

    /**
     * Returns the collection with the given user blessing and name.
     * <p>
     * This is a non-blocking method.
     *
     * @param collectionId Id of the collection.
     * @return The handle {@link Collection} to the collection
     */
    Collection getCollection(Id collectionId);

    /**
     * Returns the collection with the given relative name.
     * The user blessing is derived from the context.
     * <p>
     * This is a non-blocking method.
     *
     * @param context Vanadium context
     * @param name    Name of the collection
     * @return The handle {@link Collection} to the collection
     */
    Collection getCollection(VContext context, String name);

    /**
     * Returns a new {@link ListenableFuture} whose result is a list of all collection ids that the
     * caller is allowed to see. The list is sorted by blessing, then by name.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     * @return a list of all collection ids
     */
    @CheckReturnValue
    ListenableFuture<List<Id>> listCollections(VContext context);

    /**
     * Executes a SyncQL query, returning a {@link QueryResults} object that allows the caller to
     * iterate over arrays of values for each row that matches the query.
     * <p>
     * It is legal to perform writes concurrently with {@link #exec exec()}. The returned stream reads
     * from a consistent snapshot taken at the time of the method and will not reflect subsequent
     * writes to keys not yet reached by the stream.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     * @param query   a SyncQL query
     * @return a {@link ListenableFuture} whose result is a {@link QueryResults} object
     * that allows the caller to iterate over arrays of values for each row that
     * matches the query
     */
    @CheckReturnValue
    ListenableFuture<QueryResults> exec(VContext context, String query);

    /**
     * Executes a SyncQL parameterized query. Same as {@link #exec(VContext, String)}, with the
     * query supporting positional parameters. {@code paramValues} and {@code paramTypes} must each
     * have one element corresponding to each '?' placeholder in the query.
     *
     * @param context     Vanadium context
     * @param query       a SyncQL query
     * @param paramValues SyncQL query parameters, one per '?' placeholder in the query
     * @param paramTypes  SyncQL query parameter types, one per parameter in paramValues
     * @return a {@link ListenableFuture} whose result is a {@link QueryResults}
     * object that allows the caller to iterate over arrays of values for each
     * row that matches the query
     */
    @CheckReturnValue
    ListenableFuture<QueryResults> exec(VContext context, String query,
                                        List<Object> paramValues, List<Type> paramTypes);

    /**
     * Returns a new {@link ListenableFuture} whose result is the {@link ResumeMarker} that points
     * to the current state of the database.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     */
    @CheckReturnValue
    ListenableFuture<ResumeMarker> getResumeMarker(VContext context);

    /**
     * An interface for iterating through rows resulting from a
     * {@link DatabaseCore#exec DatabaseCore.exec()}.
     */
    interface QueryResults extends InputChannel<List<VdlAny>> {
        /**
         * Returns an array of column names that matched the query.  The size of the {@link VdlAny}
         * list returned in every {@link #recv} iteration will match the size of this list.
         */
        List<String> columnNames();
    }
}
