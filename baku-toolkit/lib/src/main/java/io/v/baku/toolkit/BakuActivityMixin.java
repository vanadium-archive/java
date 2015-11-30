// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

/**
 * An optional convenience interface for classes mixing in {@link BakuActivityTrait}.
 */
public interface BakuActivityMixin {
    BakuActivityTrait getBakuActivityTrait();
}
