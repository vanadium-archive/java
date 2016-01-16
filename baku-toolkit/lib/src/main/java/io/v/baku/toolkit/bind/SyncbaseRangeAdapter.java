// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ListView;

import com.google.common.collect.Ordering;

import io.v.rx.syncbase.RangeWatchBatch;
import io.v.rx.syncbase.RxTable;
import io.v.v23.syncbase.nosql.PrefixRange;
import io.v.v23.syncbase.nosql.RowRange;
import java8.util.function.Function;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.UtilityClass;
import rx.Observable;
import rx.functions.Func1;

@Accessors(prefix = "m")
@UtilityClass
public class SyncbaseRangeAdapter<T> {
    /**
     * If {@code T} is {@link Comparable}, the default row ordering is natural ordering on row
     * values. Otherwise, the default is natural ordering on row names.
     */
    @Accessors(prefix = "m")
    public static class Builder<T, A extends RangeAdapter>
            extends BaseBuilder<Builder<T, ? extends RangeAdapter>> {
        private PrefixRange mPrefix = RowRange.prefix("");
        private Class<T> mType;
        private Ordering<? super RxTable.Row<T>> mOrdering;
        private Func1<String, Boolean> mKeyFilter;
        private ViewAdapter<? super RxTable.Row<T>, ?> mViewAdapter;
        private Context mViewAdapterContext;

        @Getter
        private A mAdapter;

        public Builder<T, A> prefix(final PrefixRange prefix) {
            mPrefix = prefix;
            return this;
        }

        public Builder<T, A> prefix(final String prefix) {
            return prefix(RowRange.prefix(prefix));
        }

        /**
         * This setter is minimally typesafe; after setting the {@code type}, clients should
         * probably also update {@code ordering} and {@code viewAdapter}.
         */
        public <U> Builder<U, A> type(final Class<U> type) {
            @SuppressWarnings("unchecked")
            final Builder<U, A> casted =
                    (Builder<U, A>) this;
            casted.mType = type;
            return casted;
        }

        public Builder<T, A> ordering(final Ordering<? super RxTable.Row<? extends T>> ordering) {
            mOrdering = ordering;
            return this;
        }

        public Builder<T, A> valueOrdering(final Ordering<? super T> ordering) {
            return ordering(ordering.onResultOf(RxTable.Row::getValue));
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

        public Builder<T, A> keyFilter(final Func1<String, Boolean> keyFilter) {
            mKeyFilter = keyFilter;
            return this;
        }

        public Builder<T, A> viewAdapter(final ViewAdapter<? super RxTable.Row<T>, ?> viewAdapter) {
            mViewAdapter = viewAdapter;
            return this;
        }

        public <U> Builder<T, A> textViewAdapter(final Function<RxTable.Row<T>, U> fn) {
            return viewAdapter(getDefaultViewAdapter(fn));
        }

        public Builder<T, A> valueAdapter(final ViewAdapter<T, ?> viewAdapter) {
            return viewAdapter(new TransformingViewAdapter<>(viewAdapter, RxTable.Row::getValue));
        }

        private <U> ViewAdapter<? super RxTable.Row<T>, ?> getDefaultViewAdapter(
                final Function<RxTable.Row<T>, U> fn) {
            return new TextViewAdapter<U>(getDefaultViewAdapterContext()).map(fn);
        }

        /**
         * The default view adapter stringizes values.
         */
        private ViewAdapter<? super RxTable.Row<T>, ?> getDefaultViewAdapter() {
            return getDefaultViewAdapter(RxTable.Row::getValue);
        }

        public Builder<T, A> viewAdapterContext(final Context context) {
            mViewAdapterContext = context;
            return this;
        }

        public Context getDefaultViewAdapterContext() {
            return mViewAdapterContext == null ? mActivity : mViewAdapterContext;
        }

        public Observable<RangeWatchBatch<T>> buildWatch() {
            if (mType == null) {
                throw new IllegalStateException("Missing required type property");
            }
            return mRxTable.watch(mPrefix, mKeyFilter, mType);
        }

        private Ordering<? super RxTable.Row<T>> getOrdering() {
            return mOrdering == null ? getDefaultOrdering() : mOrdering;
        }

        private ViewAdapter<? super RxTable.Row<T>, ?> getViewAdapter() {
            return mViewAdapter == null ? getDefaultViewAdapter() : mViewAdapter;
        }

        private <U extends RangeAdapter> U subscribeAdapter(final U adapter) {
            subscribe(adapter.getSubscription());
            mAdapter = null;
            return adapter;
        }

        public Observable<PrefixListAccumulator<T>> buildPrefixListAccumulator() {
            return new PrefixListAccumulator<>(getOrdering())
                    .scanFrom(buildWatch());
        }

        public SyncbaseListAdapter<T> buildListAdapter() {
            return subscribeAdapter(new SyncbaseListAdapter<>(
                    buildPrefixListAccumulator(), getViewAdapter(), mOnError));
        }

        public SyncbaseRecyclerAdapter<T, ?> buildRecyclerAdapter() {
            return subscribeAdapter(new SyncbaseRecyclerAdapter<>(
                    buildPrefixListAccumulator(), getViewAdapter(), mOnError));
        }

        public Builder<T, SyncbaseListAdapter<T>> bindTo(final ListView listView) {
            @SuppressWarnings("unchecked")
            final Builder<T, SyncbaseListAdapter<T>> casted =
                    (Builder<T, SyncbaseListAdapter<T>>) this;
            casted.mAdapter = buildListAdapter();
            listView.setAdapter(casted.mAdapter);
            return casted;
        }

        public Builder<T, SyncbaseRecyclerAdapter<T, ?>> bindTo(final RecyclerView recyclerView) {
            @SuppressWarnings("unchecked")
            final Builder<T, SyncbaseRecyclerAdapter<T, ?>> casted =
                    (Builder<T, SyncbaseRecyclerAdapter<T, ?>>) this;
            casted.mAdapter = buildRecyclerAdapter();
            recyclerView.setAdapter(casted.mAdapter);
            return casted;
        }

        @Override
        public Builder<T, ?> bindTo(final View view) {
            if (view instanceof ListView) {
                return bindTo((ListView) view);
            } else if (view instanceof RecyclerView) {
                return bindTo((RecyclerView) view);
            } else {
                throw new IllegalArgumentException("No default binding for view " + view);
            }
        }
    }

    public static <T, A extends RangeAdapter> Builder<T, A> builder() {
        return new Builder<>();
    }
}
