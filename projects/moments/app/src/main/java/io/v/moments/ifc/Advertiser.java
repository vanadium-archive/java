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
     * This state is persisted.  This can be true when isAdvertising is false,
     * and vice-versa.  E.g., phone rotates, restores state, and uses this value
     * to decide if a call to  advertiseStart is warranted.
     */
    boolean shouldBeAdvertising();

    /**
     * Changes the value returned by shouldBeAdvertising.
     */
    void setShouldBeAdvertising(boolean value);

    /**
     * True if advertiseStop could usefully be called.
     */
    boolean isAdvertising();

    /**
     * Synchronously stop advertising.
     */
    void advertiseStop();
}
