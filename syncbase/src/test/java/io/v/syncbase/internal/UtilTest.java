// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class UtilTest {
    @Before
    public void setUp() {
        System.loadLibrary("syncbase");
    }

    @Test
    public void testEncode() {
        assertEquals("string", Util.Encode("string"));
        assertEquals("part1%2Fpart2", Util.Encode("part1/part2"));
        assertEquals("part1%25part2", Util.Encode("part1%part2"));
    }

    @Test
    public void testEncodeId() {
        assertEquals("blessing,name", Util.EncodeId(new Id("blessing", "name")));
    }

    @Test
    public void testNamingJoin() {
        assertEquals("a/b/c", Util.NamingJoin(Arrays.asList("a", "b", "c")));
    }
}
