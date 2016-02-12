// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.v23.ifc;

import com.google.common.util.concurrent.FutureCallback;

import io.v.v23.InputChannelCallback;
import io.v.v23.discovery.Update;

/**
 * Scanner controls, intended to be similar to Advertiser controls.
 */
public interface Scanner {
    /**
     * Asynchronously start scanning.
     *
     * Callbacks can be expected to run on the UX thread.
     *
     * @param startupCallback    executed on success or failure of scan
     *                           startup.
     * @param updateCallback     executed on each scan update (each found or
     *                           lost advertisement).
     * @param completionCallback executed on success or failure of scan
     *                           completion.  A scan might shutdown for reasons
     *                           other than a call to stop, e.g. a timeout.
     */
    void start(FutureCallback<Void> startupCallback,
               InputChannelCallback<Update> updateCallback,
               FutureCallback<Void> completionCallback);

    /**
     * True if stop could usefully be called.
     */
    boolean isScanning();

    /**
     * Synchronously stop scanning.  Should result in execution of
     * completionCallback.
     */
    void stop();
}
