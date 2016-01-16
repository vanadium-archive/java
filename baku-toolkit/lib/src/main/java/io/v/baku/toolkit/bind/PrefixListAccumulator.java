// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import com.google.common.collect.Ordering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.v.rx.syncbase.RangeWatchBatch;
import io.v.rx.syncbase.RangeWatchEvent;
import io.v.rx.syncbase.RxTable;
import io.v.v23.syncbase.nosql.ChangeType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func2;

/**
 * This class accumulates prefix watch streams into observable lists. It is meant to be used in
 * conjunction with {@link Observable#scan(Object, Func2)}:
 *
 * <p>{@code .scan(new PrefixListAccumulator<>(...), PrefixListAccumulator::add)}
 * @param <T>
 */
@Accessors(prefix = "m")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PrefixListAccumulator<T> {
    private static final String ERR_INCONSISTENT = "Sorted data are inconsistent with map data";

    public static final PrefixListAccumulator<Object> EMPTY = new PrefixListAccumulator<>(
            Collections.emptyMap(), Collections.emptyList(), Ordering.arbitrary());

    @SuppressWarnings("unchecked")
    public static <T> PrefixListAccumulator<T> empty() {
        return (PrefixListAccumulator<T>)EMPTY;
    }

    private final Map<String, T> mRows;
    private final List<RxTable.Row<T>> mSorted;
    private final Ordering<? super RxTable.Row<T>> mOrdering;

    public PrefixListAccumulator(final Ordering<? super RxTable.Row<T>> ordering) {
        mRows = new HashMap<>();
        mSorted = new ArrayList<>();
        // ensure deterministic ordering by always applying secondary order on row name
        mOrdering = ordering.compound(Ordering.natural().onResultOf(RxTable.Row::getRowName));
    }

    public Observable<PrefixListAccumulator<T>> scanFrom(
            final Observable<RangeWatchBatch<T>> watch) {
        return watch
                .concatMap(RangeWatchBatch::collectChanges)
                .observeOn(AndroidSchedulers.mainThread()) // required unless we copy
                .scan(this, PrefixListAccumulator::add);
    }

    private int findRowForEdit(final String rowName, final T oldValue) {
        final int oldIndex = Collections.binarySearch(mSorted,
                new RxTable.Row<>(rowName, oldValue), mOrdering);
        if (oldIndex < 0) {
            throw new ConcurrentModificationException(ERR_INCONSISTENT);
        } else {
            return oldIndex;
        }
    }

    private PrefixListAccumulator<T> add(final Collection<RangeWatchEvent<T>> events) {
        // TODO(rosswang): more efficient updates for larger batches
        for (final RangeWatchEvent<T> e : events) {
            if (e.getChangeType() == ChangeType.DELETE_CHANGE) {
                removeOne(e.getRow());
            } else {
                updateOne(e.getRow());
            }
        }
        return this;
    }

    protected void removeOne(final RxTable.Row<T> entry) {
        final T old = mRows.remove(entry.getRowName());
        if (old != null) {
            mSorted.remove(findRowForEdit(entry.getRowName(), old));
        }
    }

    private int insertionIndex(final RxTable.Row<T> entry) {
        final int bs = Collections.binarySearch(mSorted, entry, mOrdering);
        return bs < 0 ? ~bs : bs;
    }

    protected void updateOne(final RxTable.Row<T> entry) {
        final T old = mRows.put(entry.getRowName(), entry.getValue());
        if (old == null) {
            mSorted.add(insertionIndex(entry), entry);
        } else {
            final int oldIndex = findRowForEdit(entry.getRowName(), old);
            int newIndex = insertionIndex(entry);

            if (newIndex >= oldIndex) {
                newIndex--;
                for (int i = oldIndex; i < newIndex; i++) {
                    mSorted.set(i, mSorted.get(i + 1));
                }
            } else {
                for (int i = oldIndex; i > newIndex; i--) {
                    mSorted.set(i, mSorted.get(i - 1));
                }
            }
            mSorted.set(newIndex, entry);
        }
    }

    public int getCount() {
        return mRows.size();
    }

    public RxTable.Row<T> getRowAt(final int position) {
        return mSorted.get(position);
    }

    public T getValue(final String rowName) {
        return mRows.get(rowName);
    }

    public int getRowIndex(final String rowName) {
        return Collections.binarySearch(mSorted, new RxTable.Row<>(rowName, mRows.get(rowName)),
                mOrdering);
    }

    public boolean containsRow(final String rowName) {
        return mRows.containsKey(rowName);
    }
}
