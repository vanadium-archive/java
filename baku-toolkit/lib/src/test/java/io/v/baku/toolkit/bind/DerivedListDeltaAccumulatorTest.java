// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.support.v7.widget.RecyclerView;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;

import java8.util.function.Consumer;
import rx.Observable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RecyclerView.Adapter.class)
public class DerivedListDeltaAccumulatorTest {
    @SafeVarargs
    private static List<DerivedListDeltaAccumulator<String>> deriveDeltas(
            final ImmutableList<String>... idLists) {
        return ImmutableList.copyOf(DerivedListDeltaAccumulator.scanFrom(
                Observable.from(idLists)
                        .map(IdListAccumulator::new),
                IdListAccumulator::new)
                .toBlocking().toIterable());
    }

    private static void verifyDelta(final DerivedListDeltaAccumulator<String> delta,
                               final ImmutableList<String> expectedStep,
                               final Consumer<RecyclerView.Adapter<?>> expectedNotification) {
        assertEquals(expectedStep, delta.getListSnapshot());
        final RecyclerView.Adapter<?> rva = mock(RecyclerView.Adapter.class);
        delta.notifyDeltas(rva);
        expectedNotification.accept(verify(rva));
    }

    @Test
    public void test() {
        final List<DerivedListDeltaAccumulator<String>> deltas = deriveDeltas(
                ImmutableList.of("a", "b", "c", "d", "e"),
                ImmutableList.of("a", "c"),
                ImmutableList.of("b", "l", "a", "c", "k"),
                ImmutableList.of("k", "a", "c", "b", "l"));
        verifyDelta(deltas.get(0), ImmutableList.of("a", "b", "c", "d", "e"),
                RecyclerView.Adapter::notifyDataSetChanged);
        verifyDelta(deltas.get(1), ImmutableList.of("a", "c", "d", "e"),
                rva -> rva.notifyItemRangeRemoved(1, 1));
        verifyDelta(deltas.get(2), ImmutableList.of("a", "c"),
                rva -> rva.notifyItemRangeRemoved(2, 2));
        verifyDelta(deltas.get(3), ImmutableList.of("b", "l", "a", "c"),
                rva -> rva.notifyItemRangeInserted(0, 2));
        verifyDelta(deltas.get(4), ImmutableList.of("b", "l", "a", "c", "k"),
                rva -> rva.notifyItemRangeInserted(4, 1));
        verifyDelta(deltas.get(5), ImmutableList.of("k", "b", "l", "a", "c"),
                rva -> rva.notifyItemMoved(4, 0));
        verifyDelta(deltas.get(6), ImmutableList.of("k", "b", "a", "c", "l"),
                rva -> rva.notifyItemMoved(2, 4));
        verifyDelta(deltas.get(7), ImmutableList.of("k", "a", "c", "b", "l"),
                rva -> rva.notifyItemMoved(1, 3));
    }
}
