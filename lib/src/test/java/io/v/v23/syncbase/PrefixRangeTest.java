// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests the {@link PrefixRange} implementation.
 */
@RunWith(Parameterized.class)
public class PrefixRangeTest {
    private final PrefixRange range;
    private final String row;
    private final boolean expectedIsWithin;

    public PrefixRangeTest(String prefix, String row, boolean expectedIsWithin) {
        this.range = RowRange.prefix(prefix);
        this.row = row;
        this.expectedIsWithin = expectedIsWithin;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"", "aaaa", true},
                {"", "zzzz", true},
                {"", "", true},
                {"aaaa", "aaaa", true},
                {"aaaa", "aaaabb", true},
                {"aaaa", "aaaa" + '\u0000', true},
                {"aaaa", "aaab", false},
                {"aaaa", "aaa", false},
                {"aaaa", "", false},
                {"aaaa", "b", false},
        });
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
