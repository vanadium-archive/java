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
public class RxRecyclerAdapter<T, VH extends ViewHolder>
        extends RecyclerView.Adapter<RxRecyclerAdapter.ViewHolderAdapter<VH>>
        implements RangeAdapter, ListAccumulator<T> {

    public static class ViewHolderAdapter<B extends ViewHolder> extends RecyclerView.ViewHolder {
        public final B bakuViewHolder;
        public ViewHolderAdapter(final B vh) {
            super(vh.getView());
            bakuViewHolder = vh;
        }
    }

    private final ViewAdapter<? super T, VH> mViewAdapter;

    /**
     * The main purpose of this class is to capture the generic arg {@link T}. Otherwise, the
     * {@link Delegate} annotation fails to capture it and cannot implement {@link ListAccumulator}
     * with the right generic type.
     * <p>
     * While we're here, we might as well use it to override
     * {@link android.support.v7.widget.RecyclerView.Adapter#getItemId(int)}.
     */
    private abstract class Delegated implements ListAccumulator<T> {
        /**
         * Overrides {@link android.support.v7.widget.RecyclerView.Adapter#getItemId(int)}.
         */
        public abstract long getItemId(int position);
    }

    @Delegate(types = Delegated.class)
    private ListDeltaAccumulator<T> mLatestState =
            new DerivedListDeltaAccumulator<>(null, ListAccumulators.empty());

    @Getter
    private final Subscription mSubscription;

    public RxRecyclerAdapter(final Observable<? extends ListDeltaAccumulator<T>> data,
                             final ViewAdapter<? super T, VH> viewAdapter,
                             final Action1<Throwable> onError) {
        setHasStableIds(true);
        mViewAdapter = viewAdapter;
        mSubscription = data
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(d -> {
                    mLatestState = d;
                    d.notifyDeltas(this);
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
}
