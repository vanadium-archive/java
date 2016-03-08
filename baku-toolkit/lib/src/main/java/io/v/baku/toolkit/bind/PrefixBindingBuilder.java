// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import com.google.common.collect.Ordering;

import java.util.Comparator;

import io.v.rx.syncbase.RangeWatchBatch;
import io.v.rx.syncbase.RxTable;
import io.v.v23.syncbase.nosql.PrefixRange;
import io.v.v23.syncbase.nosql.RowRange;
import rx.Observable;
import rx.functions.Func1;

/**
 * Builder class for data bindings to collections derived from rows with names matching a key
 * prefix, optionally with additional filter criteria. Ordering is data driven. This type of
 * collection binding is advantageous in its simplicity, at the cost of being less flexible than
 * other collection bindings. Internally, the ordering always includes a secondary ordering on key
 * to remove ambiguity.
 * <p>
 * If {@code T} is {@link Comparable}, the default row ordering is natural ordering on row values.
 * Otherwise, the default is natural ordering on row names.
 *
 * @see BindingBuilder#onPrefix(String)
 * @see BindingBuilder#onPrefix(PrefixRange)
 */
public class PrefixBindingBuilder<T, A extends RangeAdapter>
        extends CollectionBindingBuilder<PrefixBindingBuilder<T, A>, RxTable.Row<T>, A> {
    private Class<T> mType;
    private PrefixRange mPrefix;
    private Comparator<? super RxTable.Row<T>> mOrdering;
    private Func1<String, Boolean> mKeyFilter;

    public PrefixBindingBuilder(final BindingBuilder base) {
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
     * The element type for the collection, i.e. the value type for rows matching the prefix and key
     * filter.
     * <p>
     * This setter is minimally typesafe; after setting the {@code type}, clients should
     * probably also update {@code ordering} and {@code viewAdapter}.
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

    private Class<T> getType() {
        if (mType == null) {
            throw new IllegalStateException("Missing required type property");
        }
        return mType;
    }

    /**
     * For comparable {@code T}, default to natural ordering on values. Otherwise, default to
     * natural ordering on row names.
     */
    private Ordering<? super RxTable.Row<? extends T>> getDefaultOrdering() {
        if (mOrdering == null && Comparable.class.isAssignableFrom(getType())) {
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
        return mBase.mRxTable.watch(mPrefix == null? RowRange.prefix("") : mPrefix,
                mKeyFilter, getType());
    }

    private Comparator<? super RxTable.Row<T>> getOrdering() {
        return mOrdering == null ? getDefaultOrdering() : mOrdering;
    }

    @Override
    public Observable<? extends PrefixListAccumulator<T>> buildListAccumulator() {
        return new PrefixListAccumulator<>(getOrdering())
                .scanFrom(buildPrefixWatch());
    }

    @Override
    public Observable<? extends PrefixListDeltaAccumulator<T>> buildListDeltaAccumulator() {
        return new PrefixListDeltaAccumulator<>(getOrdering())
                .scanFrom(buildPrefixWatch());
    }
}
