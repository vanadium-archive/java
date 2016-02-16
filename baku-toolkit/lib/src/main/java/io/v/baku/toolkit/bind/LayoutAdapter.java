// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class LayoutAdapter<T, VH extends ViewHolder> extends AbstractViewAdapter<T, VH> {

    private final LayoutInflater mInflater;
    @LayoutRes
    private final int mResource;

    public LayoutAdapter(final Context context, final @LayoutRes int resource) {
        this(LayoutInflater.from(context), resource);
    }

    @Override
    public View createView(final ViewGroup parent) {
        return mInflater.inflate(mResource, parent, false);
    }
}
