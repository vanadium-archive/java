// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ListView;

import java8.util.function.Function;
import lombok.RequiredArgsConstructor;
import rx.Observable;

/**
 * Encapsulates the common logic for building the widget side of a collection binding.
 *
 * @see BaseCollectionBindingBuilder
 */
@RequiredArgsConstructor
public abstract class CollectionAdapterBuilder<B extends CollectionAdapterBuilder<B, T, A>,
        T, A extends RangeAdapter> {
    @SuppressWarnings("unchecked")
    protected final B mSelf = (B)this;

    protected final CollectionBinding.Builder mBase;

    private ViewAdapter<? super T, ?> mViewAdapter;

    public B viewAdapter(
            final ViewAdapter<? super T, ?> viewAdapter) {
        mViewAdapter = viewAdapter;
        return mSelf;
    }

    public B textViewAdapter() {
        return viewAdapter(new TextViewAdapter(mBase.getDefaultViewAdapterContext()));
    }

    public B textViewAdapter(final Function<T, ?> fn) {
        return viewAdapter(new TextViewAdapter(mBase.getDefaultViewAdapterContext()).map(fn));
    }

    protected ViewAdapter<? super T, ?> getDefaultViewAdapter() {
        return new TextViewAdapter(mBase.getDefaultViewAdapterContext());
    }

    private ViewAdapter<? super T, ?> getViewAdapter() {
        return mViewAdapter == null ? getDefaultViewAdapter() : mViewAdapter;
    }

    private <U extends RangeAdapter> U subscribeAdapter(final U adapter) {
        mBase.subscribe(adapter.getSubscription());
        return adapter;
    }

    public abstract Observable<? extends ListAccumulator<T>> buildListAccumulator();

    public RxListAdapter<T> buildListAdapter() {
        return subscribeAdapter(new RxListAdapter<>(
                buildListAccumulator(), getViewAdapter(), mBase.mOnError));
    }

    public RxRecyclerAdapter<T, ?> buildRecyclerAdapter() {
        return subscribeAdapter(new RxRecyclerAdapter<>(
                buildListAccumulator(), getViewAdapter(), mBase.mOnError));
    }

    public RxListAdapter<T> bindTo(final ListView listView) {
        final RxListAdapter<T> adapter = buildListAdapter();
        listView.setAdapter(adapter);
        return adapter;
    }

    public RxRecyclerAdapter<T, ?> bindTo(final RecyclerView recyclerView) {
        final RxRecyclerAdapter<T, ?> adapter = buildRecyclerAdapter();
        recyclerView.setAdapter(adapter);
        return adapter;
    }

    public RangeAdapter bindTo(final View view) {
        if (view instanceof ListView) {
            return bindTo((ListView) view);
        } else if (view instanceof RecyclerView) {
            return bindTo((RecyclerView) view);
        } else {
            throw new IllegalArgumentException("No default binding for view " + view);
        }
    }
}
