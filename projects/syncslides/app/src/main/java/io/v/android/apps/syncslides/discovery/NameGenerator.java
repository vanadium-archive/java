// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import java.util.List;

/**
 * Generates a string for use as a V23 Service Name that won't collide with
 * an existing set.
 */
public interface NameGenerator {
    String getName(List<String> existing, String suggested);
}
