// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.v23.ifc;

import com.google.common.util.concurrent.FutureCallback;

import org.joda.time.Duration;

/**
 * Scanner - can start, stop, and restart a scan for advertisements.
 *
 * The start and stop cycle is appropriate for connection to a toggle button.
 */
public interface Scanner {
    /**
     * Asynchronously start scanning.
     *
     * Callbacks can be expected to run on the UX thread.
     *
     * @param onStart       Callback with success or failure handlers for scan
     *                      startup.  A success can switch a toggle button to
     *                      "on".
     * @param foundListener Handler executed on each found newly found
     *                      advertisement.
     * @param lostListener  Handler executed on each previously seen but now
     *                      lost advertisement.
     * @param onStop        Callback with success or failure handlers for scan
     *                      shutdown. A scan might shutdown for reasons other
     *                      than a call to stop, e.g. a timeout.  The callback
     *                      can, say, switch a toggle button back to "off".
     * @param timeout       Amount of time until the scan self-cancels.
     */
    void start(FutureCallback<Void> onStart,
               AdvertisementFoundListener foundListener,
               AdvertisementLostListener lostListener,
               FutureCallback<Void> onStop,
               Duration timeout);

    /**
     * True if stop could usefully be called.
     */
    boolean isScanning();

    /**
     * Synchronously stop scanning.  Should result in execution of onStop
     * callback.
     */
    void stop();
}
