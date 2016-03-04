// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

public class ExtensibleBuilder<T extends ExtensibleBuilder<T>> {
    @SuppressWarnings("unchecked")
    protected final T mSelf = (T)this;
}
