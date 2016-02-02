// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.Delegate;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

@Accessors(prefix = "m")
public class RxListAdapter<T> extends BaseAdapter
        implements RangeAdapter, ListAccumulator<T> {
    private final ViewAdapter<? super T, ?> mViewAdapter;
    @Delegate
    private ListAccumulator<T> mLatestState = ListAccumulators.empty();
    @Getter
    private final Subscription mSubscription;

    public RxListAdapter(final Observable<? extends ListAccumulator<T>> data,
                         final ViewAdapter<? super T, ?> viewAdapter,
                         final Action1<Throwable> onError) {
        mViewAdapter = viewAdapter;
        mSubscription = data
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(d -> {
                    mLatestState = d;
                    notifyDataSetChanged();
                }, onError);
    }

    @Override
    public void close() throws Exception {
        mSubscription.unsubscribe();
    }

    @Override
    public View getView(final int position, View view, final ViewGroup parent) {
        final T row = mLatestState.getRowAt(position);
        if (view == null) {
            view = mViewAdapter.createView(parent);
        }
        mViewAdapter.bindView(view, position, row);
        return view;
    }

    @Override
    public T getItem(int position) {
        return getRowAt(position);
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
