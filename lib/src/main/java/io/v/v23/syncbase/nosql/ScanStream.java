// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase.nosql;

import io.v.v23.services.syncbase.nosql.KeyValue;
import io.v.v23.verror.VException;

/**
 * An interface for iterating through a collection of key/value pairs (obtained via
 * {@link Table#scan Table.scan()}).
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
public interface ScanStream extends Iterable<KeyValue> {
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