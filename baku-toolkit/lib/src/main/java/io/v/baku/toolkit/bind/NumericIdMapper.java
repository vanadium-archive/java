// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import java.util.HashMap;
import java.util.Map;

import java8.util.Maps;

public class NumericIdMapper {
    private final Map<Object, Integer> mMapping = new HashMap<>();

    public int getNumericId(final Object key) {
        return mMapping.get(key);
    }

    public int assignNumericId(final Object key) {
        return Maps.computeIfAbsent(mMapping, key, k -> mMapping.size());
    }
}
