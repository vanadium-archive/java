// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.Delegate;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

@Accessors(prefix = "m")
public class SyncbaseRecyclerAdapter<T, VH extends ViewHolder>
        extends RecyclerView.Adapter<SyncbaseRecyclerAdapter.ViewHolderAdapter<VH>>
        implements RangeAdapter, ListAccumulator<T> {

    public static class ViewHolderAdapter<B extends ViewHolder> extends RecyclerView.ViewHolder {
        public final B bakuViewHolder;
        public ViewHolderAdapter(final B vh) {
            super(vh.getView());
            bakuViewHolder = vh;
        }
    }

    private final ViewAdapter<? super T, VH> mViewAdapter;
    @Delegate
    private ListAccumulator<T> mLatestState = ListAccumulators.empty();
    @Getter
    private final Subscription mSubscription;

    public SyncbaseRecyclerAdapter(final Observable<? extends ListAccumulator<T>> data,
                                   final ViewAdapter<? super T, VH> viewAdapter,
                                   final Action1<Throwable> onError) {
        mViewAdapter = viewAdapter;
        mSubscription = data
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(d -> {
                    mLatestState = d;
                    notifyDataSetChanged();
                    // TODO(rosswang): Use higher-fidelity update notifications.
                }, onError);
    }

    @Override
    public void close() throws Exception {
        mSubscription.unsubscribe();
    }

    @Override
    public ViewHolderAdapter<VH> onCreateViewHolder(final ViewGroup parent, final int viewType) {
        return new ViewHolderAdapter<>(
                mViewAdapter.createViewHolder(mViewAdapter.createView(parent)));
    }

    @Override
    public void onBindViewHolder(final ViewHolderAdapter<VH> holder, final int position) {
        mViewAdapter.bindViewHolder(
                holder.bakuViewHolder, position, mLatestState.getRowAt(position));
    }

    @Override
    public int getItemCount() {
        return getCount();
    }

    /**
     * TODO(rosswang): If this can improve UX, allot numeric IDs to row keys.
     */
    @Override
    public long getItemId(int i) {
        return RecyclerView.NO_ID;
    }
}
