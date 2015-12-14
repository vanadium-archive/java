// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;


import android.view.View;
import android.view.ViewGroup;

import java8.lang.FunctionalInterface;

@FunctionalInterface
public interface ViewAdapter<T> {
    View getView(int position, T value, View convertView, ViewGroup parent);
}