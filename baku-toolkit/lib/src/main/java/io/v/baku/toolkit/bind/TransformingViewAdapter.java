// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.view.View;
import android.view.ViewGroup;

import java8.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransformingViewAdapter<T, U, VH extends ViewHolder>
        extends AbstractViewAdapter<T, VH> {
    private final ViewAdapter<U, VH> mBase;
    private final Function<T, ? extends U> mFn;

    @Override
    public View createView(final ViewGroup parent) {
        return mBase.createView(parent);
    }

    @Override
    public VH createViewHolder(final View view) {
        return mBase.createViewHolder(view);
    }

    @Override
    public void bindViewHolder(final VH viewHolder, final int position, final T value) {
        mBase.bindViewHolder(viewHolder, position, mFn.apply(value));
    }
}
