// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.TypeToken;

import java.util.List;

import io.v.rx.syncbase.SingleWatchEvent;
import rx.Observable;

public class IdListBindingBuilder<A extends RangeAdapter>
        extends CollectionAdapterBuilder<IdListBindingBuilder<A>, String, A> {
    private String mIdListRowName;

    public IdListBindingBuilder(final CollectionBinding.Builder base) {
        super(base);
    }

    /**
     * This binding will produce lists of row name strings, which the item {@link ViewAdapter} will
     * need to bind to Syncbase rows with scalar {@link SyncbaseBinding}s.
     */
    public IdListBindingBuilder<A> idListRowName(final String idListRowName) {
        mIdListRowName = idListRowName;
        return this;
    }

    public Observable<SingleWatchEvent<ImmutableList<String>>> buildIdListWatch() {
        return mBase.mRxTable.watch(mIdListRowName, new TypeToken<List<String>>() {
        }, ImmutableList.of()).map(w -> w.map(ImmutableList::copyOf));
    }

    @Override
    public Observable<IdListAccumulator> buildListAccumulator() {
        return new IdListAccumulator()
                .scanFrom(buildIdListWatch());
    }
}
