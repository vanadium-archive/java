// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.support.v7.widget.RecyclerView;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java8.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import rx.Observable;

/**
 * This {@link ListDeltaAccumulator} derives its deltas from {@link ListAccumulator} snapshots.
 *
 * @param <T>
 */
@RequiredArgsConstructor
public class DerivedListDeltaAccumulator<T> implements ListDeltaAccumulator<T> {
    @RequiredArgsConstructor
    private static class DiffContext<T> {
        final NumericIdMapper ids;
        final Function<? super ImmutableList<T>, ? extends ListAccumulator<T>> stepFactory;

        final List<DerivedListDeltaAccumulator<T>> steps = new ArrayList<>();

        /**
         * Algorithm:
         * <ol>
         * <li>Notify of all removals.</li>
         * <li>Notify of moves to reorder intersection.</li>
         * <li>Notify of all insertions.</li>
         * </ol>
         * This algorithm does not trigger any item change events, preferring remove/insert instead.
         * Also, it fails if any items are not unique.
         */
        DiffContext<T> diff(final ListAccumulator<T> a, final ListAccumulator<T> b) {
            if (a == null) {
                steps.add(new DerivedListDeltaAccumulator<>(ids, b));
            } else {
                final ImmutableList<T>
                        aList = a.getListSnapshot(),
                        bList = b.getListSnapshot();
                final ImmutableSet<T>
                        aItems = ImmutableSet.copyOf(aList),
                        bItems = ImmutableSet.copyOf(bList);

                final List<T> mutable = processRemovals(aList, bItems);

                final List<T> to = new ArrayList<>(bList);
                to.retainAll(aItems);
                processMoves(mutable, to);

                processInsertions(mutable, bList);
            }
            return this;
        }

        ListAccumulator<T> produceStepSnapshot(final List<T> working) {
            return stepFactory.apply(ImmutableList.copyOf(working));
        }

        /**
         * @param start the mutable ordered start state
         * @param goal  the elements in the end state
         * @return the result of retaining {@code goal} from {@code start}
         */
        List<T> processRemovals(final ImmutableList<T> start, final ImmutableSet<T> goal) {
            final List<T> working = new ArrayList<>(start);
            int removalCount = 0, i = 0;

            for (final T item : start) {
                if (goal.contains(item)) {
                    processRemovals(working, i, removalCount);
                    i++;
                    removalCount = 0;
                } else {
                    removalCount++;
                }
            }
            processRemovals(working, i, removalCount);
            return working;
        }

        /**
         * Helper to {@link #processRemovals(ImmutableList, ImmutableSet)} that performs multi-item
         * removal, single-item removal, or no-ops based on contiguous scan results.
         */
        void processRemovals(final List<T> working, final int index, final int count) {
            if (count > 0) {
                working.subList(index, index + count).clear();
                steps.add(new DerivedListDeltaAccumulator<T>(ids, produceStepSnapshot(working)) {
                    @Override
                    public void notifyDeltas(RecyclerView.Adapter<?> rva) {
                        rva.notifyItemRangeRemoved(index, count);
                    }
                });
            }
        }

        /**
         * Transitions from {@code working} to {@code goal} by moving one item at a time. Moves are
         * performed in clusters of like deltas, moving smaller clusters first in the hopes that
         * larger clusters will fall into place as a result of the smaller moves. While this usually
         * results in optimal plans, there are cases where it can result in terrible plan, e.g.
         * <p>
         * 3 4 2 1 -> 3 2 4 1 -> 2 4 3 1 -> 4 2 3 1 -> 2 3 1 4 -> 1 2 3 4
         * <p>
         * I believe we can prevent this pitfall by rejecting moves that would break clusters:
         * <p>
         * 3 4 2 1 -> 1 3 4 2 -> 1 2 3 4
         * <p>
         * TODO(rosswang): Do this (and add a test).
         */
        void processMoves(final List<T> working, final List<T> goal) {
            final HashMap<T, Integer> goalIndices = new HashMap<>(goal.size());
            for (int i = 0; i < goal.size(); i++) {
                goalIndices.put(goal.get(i), i);
            }

            class MoveProcessor {
                int start, end, clusterStart, clusterDelta,
                        minClusterStart, minClusterEnd, minClusterDelta;

                boolean hasMovingCluster() {
                    return minClusterDelta != 0;
                }

                void startCluster(final int index, final int delta) {
                    clusterStart = index;
                    clusterDelta = delta;
                }

                void endCluster(final int indexAfter) {
                    if (clusterDelta == 0) {
                        if (!hasMovingCluster()) {
                            start = indexAfter;
                        } else if (indexAfter == end) {
                            end = clusterStart;
                        }
                    } else if (!hasMovingCluster() ||
                            indexAfter - clusterStart < minClusterEnd - minClusterStart) {
                        // smallest cluster; keep it as a prime candidate for moving
                        minClusterStart = clusterStart;
                        minClusterEnd = indexAfter;
                        minClusterDelta = clusterDelta;
                    }
                }

                MoveProcessor() {
                    this(0, working.size());
                }

                MoveProcessor(final int start, final int end) {
                    this.start = start;
                    this.end = end;
                    for (int i = start; i < end; i++) {
                        final int delta = goalIndices.get(working.get(i)) - i;
                        if (delta != clusterDelta) {
                            endCluster(i);
                            startCluster(i, delta);
                        }
                    }
                    endCluster(end);
                }

                DerivedListDeltaAccumulator<T> step(final int from, final int to) {
                    working.add(to, working.remove(from));
                    return new DerivedListDeltaAccumulator<T>(ids, produceStepSnapshot(working)) {
                        @Override
                        public void notifyDeltas(RecyclerView.Adapter<?> rva) {
                            rva.notifyItemMoved(from, to);
                        }
                    };
                }

                void addMinClusterSteps() {
                    if (minClusterDelta < 0) {
                        for (int i = minClusterStart; i < minClusterEnd; i++) {
                            steps.add(step(i, i + minClusterDelta));
                        }
                    } else {
                        for (int i = minClusterEnd - 1; i >= minClusterStart; i--) {
                            steps.add(step(i, i + minClusterDelta));
                        }
                    }
                }
            }

            for (MoveProcessor mp = new MoveProcessor(); mp.hasMovingCluster();
                 mp = new MoveProcessor(mp.start, mp.end)) {
                mp.addMinClusterSteps();
            }
        }

        /**
         * @param working the mutable ordered (intermediate) start state
         * @param goal    the ordered end state
         */
        void processInsertions(final List<T> working, final ImmutableList<T> goal) {
            final List<T> insert = new ArrayList<>();

            for (int i = 0; i < goal.size(); i++) {
                final T item = goal.get(i);
                final int insertAt = i - insert.size();
                if (insertAt < working.size() && item.equals(working.get(insertAt))) {
                    processInsertions(working, insertAt, insert);
                    insert.clear();
                } else {
                    insert.add(item);
                }
            }
            if (!insert.isEmpty()) {
                // Capture these ints so that we're not holding onto instances of the working lists
                // and opening us to errors if they're modified.
                final int count = insert.size(), index = goal.size() - count;
                steps.add(new DerivedListDeltaAccumulator<T>(ids, stepFactory.apply(goal)) {
                    @Override
                    public void notifyDeltas(RecyclerView.Adapter<?> rva) {
                        rva.notifyItemRangeInserted(index, count);
                    }
                });
            }
        }

        /**
         * Helper to {@link #processInsertions(List, ImmutableList)} that performs multi-item
         * insertion, single-item insertion, or no-ops based on contiguous scan results.
         */
        void processInsertions(final List<T> working, final int index, final List<T> insert) {
            if (!insert.isEmpty()) {
                working.addAll(index, insert);
                // Capture count so that we're not holding onto an instance of insert and opening us
                // to errors if (and when) it's modified.
                final int count = insert.size();
                steps.add(new DerivedListDeltaAccumulator<T>(ids, produceStepSnapshot(working)) {
                    @Override
                    public void notifyDeltas(RecyclerView.Adapter<?> rva) {
                        rva.notifyItemRangeInserted(index, count);
                    }
                });
            }
        }
    }

    private final NumericIdMapper mIds;
    @Delegate
    private final ListAccumulator<T> mSnapshot;

    public static <T> Observable<DerivedListDeltaAccumulator<T>> scanFrom(
            final Observable<? extends ListAccumulator<T>> snapshots,
            final Function<? super ImmutableList<T>, ? extends ListAccumulator<T>> stepFactory) {
        final NumericIdMapper ids = new NumericIdMapper();
        return Observable.concat(Observable.zip(
                // need to wrap null as an Observable (or Iterable) to capture generic wildcard
                // without overload ambiguity
                snapshots.startWith(Observable.just(null)),
                snapshots,
                (previous, next) -> Observable.from(new DiffContext<>(ids, stepFactory)
                        .diff(previous, next).steps)));
    }

    /**
     * The default implementation simply calls {@link RecyclerView.Adapter#notifyDataSetChanged()}.
     * However, it is fully expected that subclasses, such as the anonymous subclasses generated by
     * {@link DiffContext#diff(ListAccumulator, ListAccumulator)}, override this for more specific
     * notifications.
     */
    @Override
    public void notifyDeltas(final RecyclerView.Adapter<?> rva) {
        rva.notifyDataSetChanged();
    }

    @Override
    public long getItemId(final int position) {
        return mIds.assignNumericId(getRowAt(position));
    }
}
