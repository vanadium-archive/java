// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.ifc;

import io.v.moments.lib.Id;

/**
 * Set<Id> stripped down to just a membership test.
 */
public interface IdSet {
    boolean contains(Id id);
}
