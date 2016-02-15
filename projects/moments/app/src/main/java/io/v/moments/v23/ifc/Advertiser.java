// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.v23.ifc;

import com.google.common.util.concurrent.FutureCallback;

import org.joda.time.Duration;

/**
 * Advertiser - can start, stop, and restart advertisements.
 *
 * The start and stop cycle is appropriate for connection to a toggle button.
 */
public interface Advertiser {
    /**
     * Asynchronously start advertising.
     *
     * Callbacks can be expected to run on the UX thread.
     *
     * @param onStart Callback with success or failure handlers for advertiser
     *                startup.  A success can switch a toggle button to "on".
     * @param onStop  Callback with success or failure handlers for advertiser
     *                shutdown. An advertiser might shutdown for reasons other
     *                than a call to stop, e.g. a timeout.  The callback can,
     *                say, switch a toggle button back to "off".
     * @param timeout Amount of time until the advertisement self-cancels.
     */
    void start(FutureCallback<Void> onStart,
               FutureCallback<Void> onStop,
               Duration timeout);

    /**
     * True if stop could usefully be called.
     */
    boolean isAdvertising();

    /**
     * Synchronously stop advertising.  Should result in execution of onStop
     * callback.
     */
    void stop();
}
