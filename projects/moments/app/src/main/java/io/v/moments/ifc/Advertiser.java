// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.ifc;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Something needing to advertise itself will want an implementation of this.
 */
public interface Advertiser {
    /**
     * Asynchronously start advertising.  Callback executed on success or
     * failure of advertising startup.  The future returned on successful
     * startup should be given a callback to handle advertising shutdown.
     */
    void advertiseStart(FutureCallback<ListenableFuture<Void>> callback);

    /**
     * True if advertiseStop could usefully be called.
     */
    boolean isAdvertising();

    /**
     * Synchronously stop advertising.
     */
    void advertiseStop();
}
