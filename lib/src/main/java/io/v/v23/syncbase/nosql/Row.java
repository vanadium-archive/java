// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase.nosql;

import java.lang.reflect.Type;

import io.v.v23.context.VContext;
import io.v.v23.verror.VException;

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
     * Returns {@code true} iff this row exists and the caller has permissions
     * to access it.
     *
     * @param  ctx        Vanadium context
     * @throws VException if the row's existence couldn't be determined
     */
    boolean exists(VContext ctx) throws VException;

    /**
     * Deletes this row.
     *
     * @param  ctx        Vanadium context
     * @throws VException if the row couldn't be deleted
     */
    void delete(VContext ctx) throws VException;

    /**
     * Returns the value for this row.
     * <p>
     * Throws a {@link VException} if the row doesn't exist.
     *
     * @param  ctx        Vanadium context
     * @throws VException if the value couldn't be retrieved or if its type doesn't match the
     *                    provided type
     */
    Object get(VContext ctx, Type type) throws VException;

    /**
     * Writes the given value for this row.
     *
     * @param  ctx        Vanadium context
     * @param  value      value to write
     * @throws VException if the value couldn't be written
     */
    void put(VContext ctx, Object value, Type type) throws VException;
}