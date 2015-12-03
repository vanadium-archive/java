// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import com.google.common.util.concurrent.ListenableFuture;

import io.v.v23.security.Blessings;

public interface RefreshableBlessingsProvider extends BlessingsProvider {
    ListenableFuture<Blessings> refreshBlessings();
}
