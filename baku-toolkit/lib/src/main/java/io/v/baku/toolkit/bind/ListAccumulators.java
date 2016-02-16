// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import com.google.common.collect.ImmutableList;

import java.util.NoSuchElementException;

import io.v.rx.syncbase.RxTable;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ListAccumulators {


    public static final ListAccumulator<Object> EMPTY = new ListAccumulator<Object>(){
        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public boolean containsRow(final String rowName) {
            return false;
        }

        @Override
        public RxTable.Row<Object> getRowAt(final int position) {
            throw new NoSuchElementException("No elements in empty ListAccumulator.");
        }

        @Override
        public int getRowIndex(final String rowName) {
            return -1;
        }

        @Override
        public ImmutableList<Object> getListSnapshot() {
            return ImmutableList.of();
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> ListAccumulator<T> empty() {
        return (ListAccumulator<T>)EMPTY;
    }
}
