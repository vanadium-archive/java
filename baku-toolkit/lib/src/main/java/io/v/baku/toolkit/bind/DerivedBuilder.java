// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DerivedBuilder<T extends DerivedBuilder<T, B>, B> extends ExtensibleBuilder<T> {
    protected final B mBase;
}
