// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import com.google.common.collect.Ordering;

import io.v.rx.syncbase.RangeWatchBatch;
import io.v.rx.syncbase.RxTable;
import io.v.v23.syncbase.nosql.PrefixRange;
import io.v.v23.syncbase.nosql.RowRange;
import rx.Observable;
import rx.functions.Func1;

/**
 * If {@code T} is {@link Comparable}, the default row ordering is natural ordering on row
 * values. Otherwise, the default is natural ordering on row names.
 */
public class PrefixBindingBuilder<T, A extends RangeAdapter>
        extends CollectionAdapterBuilder<PrefixBindingBuilder<T, A>, RxTable.Row<T>, A> {
    private Class<T> mType;
    private PrefixRange mPrefix;
    private Ordering<? super RxTable.Row<T>> mOrdering;
    private Func1<String, Boolean> mKeyFilter;

    public PrefixBindingBuilder(final CollectionBinding.Builder base) {
        super(base);
    }

    public PrefixBindingBuilder<T, A> prefix(final PrefixRange prefix) {
        mPrefix = prefix;
        return this;
    }

    public PrefixBindingBuilder<T, A> prefix(final String prefix) {
        return prefix(RowRange.prefix(prefix));
    }

    /**
     * This setter is minimally typesafe; after setting the {@code type}, clients should
     * probably also update {@code ordering} and {@code viewAdapter}. If intending to use a
     * collection binding that requires a
     */
    public <U> PrefixBindingBuilder<U, A> type(final Class<U> type) {
        @SuppressWarnings("unchecked")
        final PrefixBindingBuilder<U, A> casted = (PrefixBindingBuilder<U, A>) this;
        casted.mType = type;
        return casted;
    }

    public PrefixBindingBuilder<T, A> ordering(
            final Ordering<? super RxTable.Row<? extends T>> ordering) {
        mOrdering = ordering;
        return this;
    }

    public PrefixBindingBuilder<T, A> valueOrdering(final Ordering<? super T> ordering) {
        return ordering(ordering.onResultOf(RxTable.Row::getValue));
    }

    public PrefixBindingBuilder<T, A> valueAdapter(final ViewAdapter<? super T, ?> viewAdapter) {
        return viewAdapter(new TransformingViewAdapter<>(viewAdapter, RxTable.Row::getValue));
    }

    @Override
    protected ViewAdapter<RxTable.Row<T>, ?> getDefaultViewAdapter() {
        return new TextViewAdapter(mBase.getDefaultViewAdapterContext()).map(RxTable.Row::getValue);
    }

    /**
     * For comparable {@code T}, default to natural ordering on values. Otherwise, default to
     * natural ordering on row names.
     */
    private Ordering<? super RxTable.Row<? extends T>> getDefaultOrdering() {
        if (mOrdering == null && Comparable.class.isAssignableFrom(mType)) {
            return Ordering.natural().onResultOf(r -> (Comparable) r.getValue());
        } else {
            return Ordering.natural().onResultOf(RxTable.Row::getRowName);
        }
    }

    public PrefixBindingBuilder<T, A> keyFilter(final Func1<String, Boolean> keyFilter) {
        mKeyFilter = keyFilter;
        return this;
    }

    public Observable<RangeWatchBatch<T>> buildPrefixWatch() {
        if (mType == null) {
            throw new IllegalStateException("Missing required type property");
        }
        return mBase.mRxTable.watch(mPrefix == null? RowRange.prefix("") : mPrefix,
                mKeyFilter, mType);
    }

    private Ordering<? super RxTable.Row<T>> getOrdering() {
        return mOrdering == null ? getDefaultOrdering() : mOrdering;
    }

    @Override
    public Observable<PrefixListAccumulator<T>> buildListAccumulator() {
        return new PrefixListAccumulator<>(getOrdering())
                .scanFrom(buildPrefixWatch());
    }
}
