// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import com.google.common.collect.ImmutableList;

import io.v.rx.syncbase.SingleWatchEvent;
import lombok.RequiredArgsConstructor;
import rx.Observable;

/**
 * This accumulator is not a true accumulator, but rather a first-order transformation.
 * TODO(rosswang): Rename these.
 */
@RequiredArgsConstructor
public class IdListAccumulator implements ListAccumulator<String> {
    private final ImmutableList<String> mIds;

    public IdListAccumulator() {
        this(ImmutableList.of());
    }

    public Observable<IdListAccumulator> scanFrom(
            final Observable<SingleWatchEvent<ImmutableList<String>>> watch) {
        return watch.map(w -> new IdListAccumulator(w.getValue()));
    }

    @Override
    public int getCount() {
        return mIds.size();
    }

    @Override
    public String getRowAt(final int position) {
        return mIds.get(position);
    }

    @Override
    public boolean containsRow(String rowName) {
        // TODO(rosswang): possibly index
        // Since contains and indexOf are O(n) on a list, these don't scale too well. If we ever
        // have to deal with large lists and performance becomes an issue, it might be indicated to
        // index these into a map. On the other hand, that's premature right now and would probably
        // end up being even slower for most cases, so I'm punting on that until there's evidence.
        return mIds.contains(rowName);
    }

    @Override
    public int getRowIndex(final String rowName) {
        // TODO(rosswang): possibly index
        return mIds.indexOf(rowName);
    }

    @Override
    public ImmutableList<String> getListSnapshot() {
        return mIds;
    }
}
