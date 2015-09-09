// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import io.v.v23.syncbase.nosql.RowRange;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests the {@link SingleRowRange} implementation.
 */
@RunWith(Parameterized.class)
public class SingleRowRangeTest {
    private final RowRange range;
    private final String row;
    private final boolean expectedIsWithin;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"aaaa", "aaaa", true},
                {"aaaa", "aaab", false},
                {"aaaa", "aaaaaa", false},
                {"aaaa", "aaaa" + '\u0000', false},
                {"aaab", "aaab", true},
                {"aaab", "aaaa", false},
                {"aaab", "aaaaaaaab", false},
        });
    }

    public SingleRowRangeTest(String range, String row, boolean expectedIsWithin) {
        this.range = RowRange.singleRow(range);
        this.row = row;
        this.expectedIsWithin = expectedIsWithin;
    }

    @Test
    public void testIsWithin() {
        if (expectedIsWithin) {
            assertThat(range.isWithin(row)).isTrue();
        } else {
            assertThat(range.isWithin(row)).isFalse();
        }
    }
}
