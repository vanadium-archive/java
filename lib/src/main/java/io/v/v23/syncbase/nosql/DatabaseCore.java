// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase.nosql;

import io.v.v23.context.VContext;
import io.v.v23.services.watch.ResumeMarker;
import io.v.v23.vdl.VdlAny;
import io.v.v23.verror.VException;

import java.util.List;

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
     *
     * @param  relativeName name of the table; must not contain slashes
     */
    Table getTable(String relativeName);

    /**
     * Returns a list of all table names.
     *
     * @param  ctx        Vanadium context
     * @return            a list of all table names
     * @throws VException if the list of table names couldn't be retrieved
     */
    String[] listTables(VContext ctx) throws VException;

    /**
     * Executes a SyncQL query, returning a {@link ResultStream} object that allows the caller to
     * iterate over arrays of values for each row that matches the query.
     * <p>
     * It is legal to perform writes concurrently with {@link #exec exec()}. The returned stream reads
     * from a consistent snapshot taken at the time of the method and will not reflect subsequent
     * writes to keys not yet reached by the stream.
     *
     * @param  ctx        Vanadium context
     * @param  query      a SyncQL query
     * @return            a {@link ResultStream} object that allows the caller to iterate over
     *                    arrays of values for each row that matches the query
     * @throws VException if there was an error executing the query
     */
    ResultStream exec(VContext ctx, String query) throws VException;

    /**
     * Returns the {@link ResumeMarker} that points to the current state of the database.
     *
     * @throws VException if there was an error obtaining the {@link ResumeMarker}
     */
    ResumeMarker getResumeMarker(VContext ctx) throws VException;

}