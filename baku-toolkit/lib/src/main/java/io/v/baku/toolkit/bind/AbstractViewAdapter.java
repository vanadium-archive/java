// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;


import java8.util.function.Function;

public abstract class AbstractViewAdapter<T> implements  ViewAdapter<T> {
    public <U> AbstractViewAdapter<U> map(final Function<U, T> fn) {
        return new TransformingViewAdapter<>(this, fn);
    }
}