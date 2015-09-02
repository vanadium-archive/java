// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import io.v.v23.vdl.VdlAny;
import io.v.v23.verror.VException;

import java.util.List;

/**
 * An interface for iterating through rows resulting from a
 * {@link DatabaseCore#exec DatabaseCore.exec()}.
 *
 * The {@link java.util.Iterator} returned by the {@link java.lang.Iterable#iterator iterator}:
 * <p><ul>
 *     <li>can be created <strong>only</strong> once,
 *     <li>does not support {@link java.util.Iterator#remove remove}</li>, and
 *     <li>may throw {@link RuntimeException} if the underlying syncbase read throws
 *         a {@link VException}.  The {@link RuntimeException#getCause cause} of the
 *         {@link RuntimeException} will be the said {@link VException}.</li>
 * </ul>
 */
public interface ResultStream extends Iterable<List<VdlAny>> {
    /**
     * Returns an array of column names that matched the query.  The size of the {@link VdlAny}
     * list returned in every iteration will match the size of this array.
     */
    List<String> columnNames();

    /**
     * Notifies the stream provider that it can stop producing elements.  The client must call
     * {@link #cancel} if it does not iterate through all the elements.
     * <p>
     * This method is idempotent and can be called concurrently with a thread that is iterating.
     * <p>
     * This method causes the iterator to (gracefully) terminate early.
     */
    void cancel() throws VException;
}