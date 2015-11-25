// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase.nosql;

import com.google.common.util.concurrent.ListenableFuture;

import io.v.v23.context.VContext;

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
     */
    ListenableFuture<Void> commit(VContext ctx);

    /**
     * Notifies the server that any pending changes can be discarded.
     * <p>
     * This method is not strictly required, but it may allow the server to release locks
     * or other resources sooner than if it was not called.
     *
     * @param  ctx        Vanadium context
     */
     ListenableFuture<Void> abort(VContext ctx);
}
