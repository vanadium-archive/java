// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

/**
 * Represents all rows with keys in {@code [start, limit)}.  If limit is {@code ""}, all rows with
 * keys &ge; {@code start} are included.
 */
public class RowRange {

    /**
     * Creates a new row range with keys in {@code [start, limit)}.
     */
    public static RowRange range(String start, String limit) {
        return new RowRange(start, limit);
    }

    /**
     * Creates a row range containing a single {@code row}.
     */
    public static RowRange singleRow(String row) {
        return new RowRange(row, row + "\u0000");
    }

    /**
     * Creates a new prefix range that includes all rows with the given {@code prefix}.
     */
    public static PrefixRange prefix(String prefix) {
        return new PrefixRange(prefix);
    }

    private final String start, limit;

    /**
     * Creates a new row range with keys in {@code [start, limit)}.
     * Package-private constructor for RowRange class. Static method
     * {@link #range} can be used to create instances.
     */
    RowRange(String start, String limit) {
        this.start = start;
        this.limit = limit;
    }

    /**
     * Returns {@code true} iff the provided row is inside this range.
     */
    public boolean isWithin(String row) {
        return this.start.compareTo(row) <= 0 &&
                (this.limit.isEmpty() || this.limit.compareTo(row) > 0);
    }

    /**
     * Returns the key that marks the start of the row range.
     */
    public String getStart() { return this.start; }

    /**
     * Returns the key that marks the limit of the row range.
     */
    public String getLimit() { return this.limit; }
}