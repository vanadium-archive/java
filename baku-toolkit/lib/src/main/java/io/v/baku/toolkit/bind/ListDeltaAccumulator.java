// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.support.v7.widget.RecyclerView;

/**
 * This interface adds delta notifications to {@link ListAccumulator} to allow granular updates to
 * {@link RecyclerView}s via {@link RxRecyclerAdapter}.
 */
public interface ListDeltaAccumulator<T> extends ListAccumulator<T> {
    void notifyDeltas(RecyclerView.Adapter<?> rva);
    long getItemId(int position);
}
