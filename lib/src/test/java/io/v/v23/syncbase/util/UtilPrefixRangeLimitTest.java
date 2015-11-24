// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static com.google.common.truth.Truth.assertThat;

/**
 * Unit tests for {@link Util#prefixRangeStart} and {@link Util#prefixRangeLimit}.
 */
@RunWith(Parameterized.class)
public class UtilPrefixRangeLimitTest {
    private final String prefix;
    private final String expectedStart;
    private final String expectedLimit;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"", "", ""},
                {"a", "a", "b"},
                {"aa", "aa", "ab"},
                {"\u00fe", "\u00fe", "\u00ff"},
                {"a\u00fe", "a\u00fe", "a\u00ff"},
                {"aa\u00fe", "aa\u00fe", "aa\u00ff"},
                {"a\u00ff", "a\u00ff", "b"},
                {"aa\u00ff", "aa\u00ff", "ab"},
                {"a\u00ff\u00ff", "a\u00ff\u00ff", "b"},
                {"aa\u00ff\u00ff", "aa\u00ff\u00ff", "ab"},
                {"\u00ff", "\u00ff", ""},
                {"\u00ff\u00ff", "\u00ff\u00ff", ""}
        });
    }

    public UtilPrefixRangeLimitTest(String prefix, String start, String limit) {
        this.prefix = prefix;
        this.expectedStart = start;
        this.expectedLimit = limit;
    }

    @Test
    public void testPrefixRangeStart() {
        assertThat(Util.prefixRangeStart(prefix)).isEqualTo(expectedStart);
    }

    @Test
    public void testPrefixRangeLimit() {
        assertThat(Util.prefixRangeLimit(prefix)).isEqualTo(expectedLimit);
    }
}