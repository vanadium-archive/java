// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import io.v.rx.syncbase.RxTable;
import lombok.Getter;
import lombok.experimental.Accessors;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

@Accessors(prefix = "m")
public class SyncbaseListAdapter<T> extends BaseAdapter implements RangeAdapter {
    private final ViewAdapter<? super RxTable.Row<T>, ?> mViewAdapter;
    @Getter
    private PrefixListAccumulator<T> mLatestState = PrefixListAccumulator.empty();
    @Getter
    private final Subscription mSubscription;

    public SyncbaseListAdapter(final Observable<PrefixListAccumulator<T>> data,
                               final ViewAdapter<? super RxTable.Row<T>, ?> viewAdapter,
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
    public int getCount() {
        return mLatestState.getCount();
    }

    @Override
    public View getView(final int position, View view, final ViewGroup parent) {
        final RxTable.Row<T> entry = mLatestState.getRowAt(position);
        if (view == null) {
            view = mViewAdapter.createView(parent);
        }
        mViewAdapter.bindView(view, position, entry);
        return view;
    }

    @Override
    public T getItem(int position) {
        return mLatestState.getRowAt(position).getValue();
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
