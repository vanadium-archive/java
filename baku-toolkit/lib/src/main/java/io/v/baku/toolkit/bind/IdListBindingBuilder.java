// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;

import java.util.List;

import io.v.rx.syncbase.SingleWatchEvent;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Builder class for data bindings to collections with item IDs derived from a serialized list of
 * IDs persisted as a single Syncbase row. Each item has its own set of data rows identified by
 * those IDs. This kind of ordering allows user-driven reordering of items where the ordering at any
 * time is guaranteed to have been determined by a modifying party. For example, if Alice writes
 * [1, 2, 4] and Bob writes [1, 3, 4], this strategy will resolve the conflict to either [1, 2, 4]
 * or [1, 3, 4], whereas {@linkplain PrefixBindingBuilder prefix bindings} would resolve to either
 * [1, 2, 3, 4] or [1, 3, 2, 4].
 *
 * @see BindingBuilder#onIdList(String)
 */
public class IdListBindingBuilder<A extends RangeAdapter>
        extends CollectionBindingBuilder<IdListBindingBuilder<A>, String, A> {
    private String mIdListRowName;

    public IdListBindingBuilder(final BindingBuilder base) {
        super(base);
    }

    /**
     * This binding will produce lists of row name strings, which the item {@link ViewAdapter} will
     * need to bind to Syncbase rows with {@linkplain ScalarBindingBuilder scalar bindings}.
     */
    public IdListBindingBuilder<A> idListRowName(final String idListRowName) {
        mIdListRowName = idListRowName;
        return this;
    }

    /**
     * This assumes that no IDs are null.
     */
    public Observable<SingleWatchEvent<ImmutableList<String>>> buildIdListWatch() {
        return mBase.mRxTable.watch(mIdListRowName, new TypeToken<List<String>>() {
        }, ImmutableList.of()).map(w -> w.map(ImmutableList::copyOf));
    }

    @Override
    public Observable<IdListAccumulator> buildListAccumulator() {
        return new IdListAccumulator()
                .scanFrom(buildIdListWatch());
    }

    @Override
    public Observable<? extends ListDeltaAccumulator<String>> buildListDeltaAccumulator() {
        return DerivedListDeltaAccumulator.scanFrom(buildListAccumulator()
                .observeOn(Schedulers.computation()), IdListAccumulator::new);
    }
}
