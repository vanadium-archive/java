// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.v.rx.syncbase.RangeWatchBatch;
import io.v.rx.syncbase.RangeWatchEvent;
import io.v.rx.syncbase.RxTable;
import io.v.v23.syncbase.nosql.ChangeType;
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
public class PrefixListAccumulator<T> implements ListAccumulator<RxTable.Row<T>> {
    private static final String ERR_INCONSISTENT = "Sorted data are inconsistent with map data";

    private final Map<String, T> mRows = new HashMap<>();
    private final List<RxTable.Row<T>> mSorted = new ArrayList<>();
    private final Comparator<? super RxTable.Row<T>> mOrdering;

    public PrefixListAccumulator(final Comparator<? super RxTable.Row<T>> ordering) {
        // ensure deterministic ordering by always applying secondary order on row name
        mOrdering = Ordering.from(ordering).compound(
                Ordering.natural().onResultOf(RxTable.Row::getRowName));
    }

    /**
     * The generic wildcard is for the benefit of subclass overrides.
     */
    public Observable<? extends PrefixListAccumulator<T>> scanFrom(
            final Observable<RangeWatchBatch<T>> watch) {
        return watch
                .concatMap(RangeWatchBatch::collectChanges)
                .observeOn(AndroidSchedulers.mainThread()) // required unless we copy
                .scan(this, PrefixListAccumulator::withUpdates);
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

    protected PrefixListAccumulator<T> withUpdates(final Collection<RangeWatchEvent<T>> events) {
        // TODO(rosswang): more efficient updates for larger batches
        // TODO(rosswang): allow option to copy on add (immutable accumulator)
        // If we copy on add, don't forget to override the clone in PrefixListDeltaAccumulator.
        for (final RangeWatchEvent<T> e : events) {
            if (e.getChangeType() == ChangeType.DELETE_CHANGE) {
                removeOne(e.getRow());
            } else {
                updateOne(e.getRow());
            }
        }
        return this;
    }

    private void removeOne(final RxTable.Row<T> entry) {
        final T old = mRows.remove(entry.getRowName());
        if (old != null) {
            remove(findRowForEdit(entry.getRowName(), old));
        }
    }

    private int insertionIndex(final RxTable.Row<T> entry) {
        final int bs = Collections.binarySearch(mSorted, entry, mOrdering);
        return bs < 0 ? ~bs : bs;
    }

    private void updateOne(final RxTable.Row<T> entry) {
        final T old = mRows.put(entry.getRowName(), entry.getValue());
        if (old == null) {
            insert(insertionIndex(entry), entry);
        } else {
            final int oldIndex = findRowForEdit(entry.getRowName(), old);
            final int newIndex = insertionIndex(entry);
            if (oldIndex == newIndex) {
                change(newIndex, entry);
            } else {
                move(oldIndex, newIndex, entry);
            }
        }
    }

    protected void remove(final int index) {
        mSorted.remove(index);
    }

    protected void insert(final int index, final RxTable.Row<T> entry) {
        mSorted.add(index, entry);
    }

    protected void move(final int from, final int to, final RxTable.Row<T> entry) {
        mSorted.remove(from);
        mSorted.add(to, entry);
    }

    protected void change(final int index, final RxTable.Row<T> entry) {
        mSorted.set(index, entry);
    }

    @Override
    public int getCount() {
        return mRows.size();
    }

    @Override
    public RxTable.Row<T> getRowAt(final int position) {
        return mSorted.get(position);
    }

    public T getValue(final String rowName) {
        return mRows.get(rowName);
    }

    @Override
    public int getRowIndex(final String rowName) {
        return Collections.binarySearch(mSorted, new RxTable.Row<>(rowName, mRows.get(rowName)),
                mOrdering);
    }

    @Override
    public boolean containsRow(final String rowName) {
        return mRows.containsKey(rowName);
    }

    @Override
    public ImmutableList<RxTable.Row<T>> getListSnapshot() {
        return ImmutableList.copyOf(mSorted);
    }
}
