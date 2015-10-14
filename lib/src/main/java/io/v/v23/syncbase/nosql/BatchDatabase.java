// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase.nosql;

import io.v.v23.context.VContext;
import io.v.v23.rpc.Callback;
import io.v.v23.verror.VException;

/**
 * A handle to a set of reads and writes to the database that should be considered an atomic unit.
 * <p>
 * See {@link Database#beginBatch Database.beginBatch()} for concurrency semantics.
 */
public interface BatchDatabase extends DatabaseCore {
    /**
     * Persists the pending changes to the database.
     *
     * @param  ctx        Vanadium context
     * @throws VException if there was an error committing the changes
     */
    void commit(VContext ctx) throws VException;

    /**
     * Asynchronous version of {@link #commit(VContext)}.
     * @throws VException if there was an error creating the asynchronous call. In this case, no
     *                    methods on {@code callback} will be called.
     */
    void commit(VContext ctx, Callback<Void> callback) throws VException;

    /**
     * Notifies the server that any pending changes can be discarded.
     * <p>
     * This method is not strictly required, but it may allow the server to release locks
     * or other resources sooner than if it was not called.
     *
     * @param  ctx        Vanadium context
     * @throws VException if there was an error discarding the changes
     */
    void abort(VContext ctx) throws VException;

    /**
     * Asynchronous version of {@link #abort(VContext)}.
     * @throws VException if there was an error creating the asynchronous call. In this case, no
     *                    methods on {@code callback} will be called.
     */
    void abort(VContext ctx, Callback<Void> callback) throws VException;
}
