// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase.nosql;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.reflect.Type;

import io.v.v23.context.VContext;

/**
 * A handle for a single row in a {@link Table}.
 */
public interface Row {
    /**
     * Returns the primary key for this row.
     */
    String key();

    /**
     * Returns the full (i.e., object) name of this row.
     */
    String fullName();

    /**
     * Returns a new {@link ListenableFuture} whose result is {@code true} iff this row exists and
     * the caller has permissions to access it.
     *
     * @param  ctx        Vanadium context
     */
    ListenableFuture<Boolean> exists(VContext ctx);

    /**
     * Deletes this row.
     *
     * @param  ctx        Vanadium context
     */
    ListenableFuture<Void> delete(VContext ctx);

    /**
     * Returns the value for this row.
     * <p>
     * The returned {@link ListenableFuture} will fail if the row doesn't exist.
     *
     * @param  ctx        Vanadium context
     */
    ListenableFuture<Object> get(VContext ctx, Type type);

    /**
     * Writes the given value for this row.
     *
     * @param  ctx        Vanadium context
     * @param  value      value to write
     */
    ListenableFuture<Void> put(VContext ctx, Object value, Type type);
}