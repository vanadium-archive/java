// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import com.google.common.collect.ImmutableList;

import java8.util.function.Function;
import rx.Observable;

/**
 * This interface tracks list updates for use with {@link android.widget.ListView}s via
 * {@link RxListAdapter} and {@link android.support.v7.widget.RecyclerView}s via
 * {@link RxRecyclerAdapter}. To support granular update notifications for {@code RecyclerView},
 * the {@link ListDeltaAccumulator} subinterface is required. A {@code ListAccumulator} can be
 * converted to a {@code ListDeltaAccumulator} by wrapping it with
 * {@link DerivedListDeltaAccumulator#scanFrom(Observable, Function)}.
 */
public interface ListAccumulator<T> {
    boolean containsRow(String rowName);
    int getCount();
    T getRowAt(int position);

    /**
     * @return the row index, or a negative index if not present. The negative value (and whether or
     *  not it has meaning) may vary by implementation.
     */
    int getRowIndex(String rowName);

    ImmutableList<T> getListSnapshot();
}
