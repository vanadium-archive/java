// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.view.View;
import android.view.ViewGroup;

import java8.util.function.Function;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TransformingViewAdapter<T, U> extends AbstractViewAdapter<T> {
    private final ViewAdapter<U> mBase;
    private final Function<T, U> mFn;

    @Override
    public View getView(int position, T value, View convertView, ViewGroup parent) {
        return mBase.getView(position, mFn.apply(value), convertView, parent);
    }
}
