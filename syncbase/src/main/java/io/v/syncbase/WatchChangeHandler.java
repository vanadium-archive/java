// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import java.util.Iterator;

// TODO(sadovsky): Make this a nested class of Database?
public abstract class WatchChangeHandler {
    // TODO(sadovsky): Consider adopting Aaron's suggestion of combining onInitialState and
    // onChangeBatch into a single method, to make things simpler for developers who don't want to
    // apply deltas to their in-memory data structures:
    // void onChangeBatch(Iterator<WatchChange> values, Iterator<WatchChange> changes)

    void onInitialState(Iterator<WatchChange> values) {
    }

    void onChangeBatch(Iterator<WatchChange> changes) {
    }

    void onError(Exception e) {
    }
}
