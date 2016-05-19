// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import io.v.v23.services.watch.Change;

public class WatchChange {
    // TODO(sadovsky): Fill this in further.

    // TODO(sadovsky): Eliminate the code below once we've switched to io.v.syncbase.core.

    protected WatchChange(Change c) {
        throw new RuntimeException("Not implemented");
    }

    protected Change toVChange() {
        throw new RuntimeException("Not implemented");
    }
}