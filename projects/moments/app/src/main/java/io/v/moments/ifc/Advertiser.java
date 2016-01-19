// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.ifc;

/**
 * Something needing to advertise itself will want an implementation of this.
 */
public interface Advertiser {
    /**
     * Synchronously start advertising.
     */
    void advertiseStart();

    /**
     * True if advertiseStop could usefully be called.
     */
    boolean isAdvertising();

    /**
     * Synchronously stop advertising.
     */
    void advertiseStop();
}
