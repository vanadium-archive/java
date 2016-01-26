// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

public interface ListAccumulator<T> {
    boolean containsRow(String rowName);
    int getCount();
    T getRowAt(int position);

    /**
     * @return the row index, or a negative index if not present. The negative value (and whether or
     *  not it has meaning) may vary by implementation.
     */
    int getRowIndex(String rowName);
}
