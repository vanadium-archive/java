// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.google.common.collect.Ordering;

import java.util.Collection;

import io.v.rx.syncbase.RangeWatchBatch;
import io.v.rx.syncbase.RangeWatchEvent;
import io.v.rx.syncbase.RxTable;
import lombok.experimental.Accessors;
import lombok.experimental.Delegate;
import rx.Observable;
import rx.functions.Action1;

@Accessors(prefix = "m")
public class SyncbaseListAdapter<T> extends BaseAdapter implements RangeAdapter {
    @Delegate
    private final SyncbaseRangeAdapter<T> mAdapter;
    private final ViewAdapter<? super RxTable.Row<T>, ?> mViewAdapter;

    public SyncbaseListAdapter(final Observable<RangeWatchBatch<T>> watch,
                               final Ordering<? super RxTable.Row<T>> ordering,
                               final ViewAdapter<? super RxTable.Row<T>, ?> viewAdapter,
                               final Action1<Throwable> onError) {
        mAdapter = new SyncbaseRangeAdapter<T>(watch, ordering, onError) {
            @Override
            protected void processEvents(Collection<RangeWatchEvent<T>> rangeWatchEvents) {
                super.processEvents(rangeWatchEvents);
                notifyDataSetChanged();
            }
        };
        mViewAdapter = viewAdapter;
    }

    @Override
    public View getView(final int position, View view, final ViewGroup parent) {
        final RxTable.Row<T> entry = mAdapter.getRowAt(position);
        if (view == null) {
            view = mViewAdapter.createView(parent);
        }
        mViewAdapter.bindView(view, position, entry);
        return view;
    }

    @Override
    public T getItem(int position) {
        return mAdapter.getRowAt(position).getValue();
    }

    /**
     * TODO(rosswang): If this can improve UX, allot numeric IDs to row keys.
     * @return a dummy row ID for the item at the requested position.
     */
    @Override
    public long getItemId(int i) {
        return 0;
    }
}
