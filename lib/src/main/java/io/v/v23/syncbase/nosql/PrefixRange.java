// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import io.v.v23.syncbase.util.Util;

/**
 * Represents all rows with keys that have some prefix.
 */
public class PrefixRange extends RowRange {
    private final String prefix;

    /**
     * Creates a new prefix range that includes all rows with the given prefix.
     */
    public PrefixRange(String prefix) {
        super(Util.prefixRangeStart(prefix), Util.prefixRangeLimit(prefix));
        this.prefix = prefix;
    }

    /**
     * Returns the prefix shared by all the keys in the range.
     */
    public String getPrefix() {
        return this.prefix;
    }
}