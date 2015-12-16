// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

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
public class SyncbaseRecyclerAdapter<T, VH extends ViewHolder>
        extends RecyclerView.Adapter<SyncbaseRecyclerAdapter.ViewHolderAdapter<VH>>
        implements RangeAdapter {

    public static class ViewHolderAdapter<B extends ViewHolder> extends RecyclerView.ViewHolder {
        public final B bakuViewHolder;
        public ViewHolderAdapter(final B vh) {
            super(vh.getView());
            bakuViewHolder = vh;
        }
    }

    private interface SimilarButDifferent {
        int getCount();
    }

    @Delegate(excludes = SimilarButDifferent.class)
    private final SyncbaseRangeAdapter<T> mAdapter;
    private final ViewAdapter<? super RxTable.Row<T>, VH> mViewAdapter;

    public SyncbaseRecyclerAdapter(final Observable<RangeWatchBatch<T>> watch,
                                   final Ordering<? super RxTable.Row<T>> ordering,
                                   final ViewAdapter<? super RxTable.Row<T>, VH> viewAdapter,
                                   final Action1<Throwable> onError) {
        mAdapter = new SyncbaseRangeAdapter<T>(watch, ordering, onError) {
            @Override
            protected void processEvents(Collection<RangeWatchEvent<T>> rangeWatchEvents) {
                super.processEvents(rangeWatchEvents);
                notifyDataSetChanged();
                // TODO(rosswang): Use higher-fidelity update notifications.
            }
        };
        mViewAdapter = viewAdapter;
    }

    @Override
    public ViewHolderAdapter<VH> onCreateViewHolder(final ViewGroup parent, final int viewType) {
        return new ViewHolderAdapter<>(
                mViewAdapter.createViewHolder(mViewAdapter.createView(parent)));
    }

    @Override
    public void onBindViewHolder(final ViewHolderAdapter<VH> holder, final int position) {
        mViewAdapter.bindViewHolder(holder.bakuViewHolder, position, mAdapter.getRowAt(position));
    }

    /**
     * TODO(rosswang): If this can improve UX, allot numeric IDs to row keys.
     */
    @Override
    public long getItemId(int i) {
        return RecyclerView.NO_ID;
    }

    @Override
    public int getItemCount() {
        return mAdapter.getCount();
    }
}
