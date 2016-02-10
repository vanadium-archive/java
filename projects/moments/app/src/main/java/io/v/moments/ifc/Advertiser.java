// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.ifc;

import com.google.common.util.concurrent.FutureCallback;

/**
 * Advertiser controls, intended to be similar to Scanner controls.
 */
public interface Advertiser {
    /**
     * Asynchronously start advertising.
     *
     * Callbacks can be expected to run on the UX thread.
     *
     * @param startupCallback    executed on success or failure of advertising
     *                           startup.
     * @param completionCallback executed on success or failure of advertising
     *                           completion.  An advertisement might shutdown
     *                           for reasons other than a call to stop, e.g. a
     *                           timeout.
     */
    void start(FutureCallback<Void> startupCallback,
               FutureCallback<Void> completionCallback);

    /**
     * True if stop could usefully be called.
     */
    boolean isAdvertising();

    /**
     * Synchronously stop advertising.  Should result in execution of
     * completionCallback.
     */
    void stop();
}
