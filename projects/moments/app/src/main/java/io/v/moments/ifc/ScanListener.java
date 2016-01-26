// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.ifc;

import io.v.v23.discovery.Update;

/**
 * Implementations of this interface can be registered with
 * {@link io.v.moments.lib.V23Manager#scan} to receive notifications when discovery events occur.
 */
public interface ScanListener {
    void scanUpdateReceived(Update result);
}
