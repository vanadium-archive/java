// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.support.annotation.IdRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ListView;

import io.v.baku.toolkit.BakuActivityTrait;
import java8.util.function.Function;
import rx.Observable;

/**
 * Collection bindings are read-only bindings from Syncbase data to collections of UI elements, such
 * as items in a {@link ListView} or {@link RecyclerView}. In addition to defining the Syncbase data
 * being mapped, collection bindings are responsible for ordering/arranging the data and mapping
 * them to their view elements (they are Android widget adapters). Writes are generally done through
 * {@linkplain BakuActivityTrait#getSyncbaseTable() direct database writes}.
 */
public abstract class CollectionBindingBuilder<B extends CollectionBindingBuilder<B, T, A>,
        T, A extends RangeAdapter> extends DerivedBuilder<B, BindingBuilder>{
    public CollectionBindingBuilder(final BindingBuilder base) {
        super(base);
    }

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
    public abstract Observable<? extends ListDeltaAccumulator<T>> buildListDeltaAccumulator();

    public RxListAdapter<T> buildListAdapter() {
        return subscribeAdapter(new RxListAdapter<>(
                buildListAccumulator(), getViewAdapter(), mBase.mOnError));
    }

    public RxRecyclerAdapter<T, ?> buildRecyclerAdapter() {
        return subscribeAdapter(new RxRecyclerAdapter<>(
                buildListDeltaAccumulator(), getViewAdapter(), mBase.mOnError));
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

    /**
     * Binds to the view identified by {@code viewId}.
     * @see #bindTo(View)
     */
    public RangeAdapter bindTo(final @IdRes int viewId) {
        return bindTo(mBase.mActivity.findViewById(viewId));
    }
}
