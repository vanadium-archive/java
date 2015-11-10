// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.libs.discovery.ble;

import io.v.impl.google.lib.discovery.ScanHandler;
import io.v.x.ref.lib.discovery.Advertisement;

/**
 * An implementation of the ScanHandler for use by the discovery framework.  This handler is used
 * to pass results from the BlePlugin to the go wrapper to passed on to the discovery instance.
 */
class NativeScanHandler implements ScanHandler{
    /**
     * A pointer to the the native channel.
     */
    private long nativeChan;

    NativeScanHandler(long nativeChan) {
        this.nativeChan = nativeChan;
    }

    private native void nativeHandleUpdate(Advertisement adv, long chan);
    private native void nativeFinalize(long chan);

    @Override
    public void handleUpdate(Advertisement advertisement) {
        nativeHandleUpdate(advertisement, nativeChan);
    }

    @Override
    protected void finalize() throws Throwable {
        nativeFinalize(nativeChan);
    }
}
