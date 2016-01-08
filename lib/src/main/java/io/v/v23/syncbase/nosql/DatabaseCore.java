// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase.nosql;

import com.google.common.util.concurrent.ListenableFuture;

import io.v.v23.InputChannel;
import io.v.v23.context.VContext;
import io.v.v23.services.watch.ResumeMarker;
import io.v.v23.vdl.VdlAny;

import java.util.List;

import javax.annotation.CheckReturnValue;

/**
 * Base interface for {@link Database} and {@link BatchDatabase}, allowing clients to pass the
 * handle to helper methods that are batch-agnostic.
 */
public interface DatabaseCore {
    /**
     * Returns the relative name of the database.
     */
    String name();

    /**
     * Returns the full (i.e., object) name of the database.
     */
    String fullName();

    /**
     * Returns the table with the given name.
     * <p>
     * This is a non-blocking method.
     *
     * @param  relativeName name of the table; must not contain slashes
     */
    Table getTable(String relativeName);

    /**
     * Returns a new {@link ListenableFuture} whose result is a list of all table names.
     *
     * @param  ctx        Vanadium context
     * @return            a list of all table names
     */
    @CheckReturnValue
    ListenableFuture<List<String>> listTables(VContext ctx);

    /**
     * Executes a SyncQL query, returning a {@link QueryResults} object that allows the caller to
     * iterate over arrays of values for each row that matches the query.
     * <p>
     * It is legal to perform writes concurrently with {@link #exec exec()}. The returned stream reads
     * from a consistent snapshot taken at the time of the method and will not reflect subsequent
     * writes to keys not yet reached by the stream.
     * <p>
     * {@link io.v.v23.context.VContext#cancel Canceling} the provided context will
     * stop the query execution and terminate the returned iterator early.
     *
     * @param  ctx        Vanadium context
     * @param  query      a SyncQL query
     * @return            a {@link ListenableFuture} whose result is a {@link QueryResults} object
     *                    that allows the caller to iterate over arrays of values for each row that
     *                    matches the query
     */
    @CheckReturnValue
    ListenableFuture<QueryResults> exec(VContext ctx, String query);

    /**
     * Returns a new {@link ListenableFuture} whose result is the {@link ResumeMarker} that points
     * to the current state of the database.
     */
    @CheckReturnValue
    ListenableFuture<ResumeMarker> getResumeMarker(VContext ctx);

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