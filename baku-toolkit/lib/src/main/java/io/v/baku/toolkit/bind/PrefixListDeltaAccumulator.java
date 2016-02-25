// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.support.v7.widget.RecyclerView;

import java.util.Comparator;

import io.v.rx.syncbase.RangeWatchBatch;
import io.v.rx.syncbase.RxTable;
import java8.util.function.Consumer;
import rx.Observable;

/**
 * This variant of {@link PrefixListAccumulator} notifies
 * {@link android.support.v7.widget.RecyclerView.Adapter}s of granular data changes. For now, this
 * implementation does not treat batches any differently, and derives reordering directly from
 * one-by-one sorts. TODO(rosswang): Do we need to optimize this?
 */
public class PrefixListDeltaAccumulator<T> extends PrefixListAccumulator<T>
        implements ListDeltaAccumulator<RxTable.Row<T>> {
    private Consumer<RecyclerView.Adapter> mDeltas;
    private final NumericIdMapper mIds = new NumericIdMapper();

    public PrefixListDeltaAccumulator(final Comparator<? super RxTable.Row<T>> ordering) {
        super(ordering);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Observable<? extends PrefixListDeltaAccumulator<T>> scanFrom(
            final Observable<RangeWatchBatch<T>> watch) {
        return (Observable<? extends PrefixListDeltaAccumulator<T>>)super.scanFrom(watch);
    }

    @Override
    protected void remove(final int index) {
        super.remove(index);
        mDeltas = a -> a.notifyItemRemoved(index);
    }

    @Override
    protected void insert(final int index, final RxTable.Row<T> entry) {
        super.insert(index, entry);
        mDeltas = a -> a.notifyItemInserted(index);
        mIds.assignNumericId(entry.getRowName());
    }

    @Override
    protected void move(final int from, final int to, final RxTable.Row<T> entry) {
        super.move(from, to, entry);
        mDeltas = a -> a.notifyItemMoved(from, to);
    }

    @Override
    protected void change(final int index, final RxTable.Row<T> entry) {
        super.change(index, entry);
        // TODO(rosswang): Can we do anything with passing the optional payload here?
        mDeltas = a -> a.notifyItemChanged(index);
    }

    @Override
    public void notifyDeltas(final RecyclerView.Adapter<?> rva) {
        if (mDeltas != null) {
            mDeltas.accept(rva);
        }
    }

    @Override
    public long getItemId(final int position) {
        return mIds.getNumericId(getRowAt(position).getRowName());
    }
}
