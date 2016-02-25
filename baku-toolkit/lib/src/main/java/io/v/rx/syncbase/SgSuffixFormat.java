// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx.syncbase;

import java8.lang.FunctionalInterface;

@FunctionalInterface
public interface SgSuffixFormat<T> {
    String get(final T parameters);
}
