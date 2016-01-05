// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.ifc;

import io.v.moments.lib.Id;

/**
 * Something with an ID.
 */
public interface HasId {
    Id getId();
}
