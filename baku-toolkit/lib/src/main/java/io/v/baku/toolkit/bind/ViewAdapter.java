// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;


import android.view.View;
import android.view.ViewGroup;

import java8.lang.FunctionalInterface;

@FunctionalInterface
public interface ViewAdapter<T, VH extends ViewHolder> {
    View createView(ViewGroup parent);
    VH createViewHolder(View view);
    void bindViewHolder(VH viewHolder, int position, T value);
    void bindView(View view, int position, T value);
}