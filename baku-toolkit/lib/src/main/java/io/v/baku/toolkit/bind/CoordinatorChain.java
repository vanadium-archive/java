// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.bind;

import java8.lang.FunctionalInterface;
import java8.util.function.UnaryOperator;

@FunctionalInterface
public interface CoordinatorChain<T> extends UnaryOperator<TwoWayBinding<T>> {
}
