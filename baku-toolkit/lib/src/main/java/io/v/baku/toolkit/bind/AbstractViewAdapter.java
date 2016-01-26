// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;


import android.view.View;

import java8.util.function.Function;

public abstract class AbstractViewAdapter<T, VH extends ViewHolder> implements ViewAdapter<T, VH> {
    @Override
    public <U> ViewAdapter<U, VH> map(final Function<U, ? extends T> fn) {
        return new TransformingViewAdapter<>(this, fn);
    }

    @Override
    public void bindView(final View view, final int position, final T value) {
        bindViewHolder(createViewHolder(view), position, value);
    }
}