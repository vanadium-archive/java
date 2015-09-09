// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests the {@link RowRange} multi-row implementation.
 */
@RunWith(Parameterized.class)
public class RowRangeMultiRowTest {
    private final RowRange range;
    private final String row;
    private final boolean expectedIsWithin;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"aaaa", "aaab", "aaaa", true},
                {"aaaa", "aaab", "aaab", false},
                {"aaaa", "aaaa", "aaaa", false},
                {"aaaa", "aaab", "aaaaa", true},
                {"aaaa", "aaaa" + '\u0000', "aaaa", true},
                {"aaaa", "aaaa" + '\u0000', "aaaaaaaaaaaa", false},
                {"aaaa", "aaaa" + '\u0000', "aaaa" + '\u0000', false},
                {"aaab", "aaac", "aaaa", false},
                {"aaab", "aaac", "aaaaaaab", false},
                {"aaaa", "", "aaaaa", true},
                {"aaaa", "", "bbbb", true},
                {"aaab", "", "aaaa", false},
        });
    }

    public RowRangeMultiRowTest(String start, String limit, String row, boolean expectedIsWithin) {
        this.range = RowRange.range(start, limit);
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
