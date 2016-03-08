// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.widget.ListView;

import org.junit.Test;

public class PrefixBindingBuilderTest {
    @Test(expected = IllegalStateException.class)
    public void testMissingType() {
        new BindingBuilder()
                .forPrefix("foo")
                .bindTo((ListView)null);
    }
}
