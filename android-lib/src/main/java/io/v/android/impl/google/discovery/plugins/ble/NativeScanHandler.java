// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.discovery.plugins.ble;

import java.util.Map;

/**
 * An implementation of the {@link Driver.ScanHandler} for use by the discovery framework.
 * <p>
 * This handler is used to pass results from a Java driver to the Go wrapper to be passed
 * on to the BLE plugin.
 */
class NativeScanHandler implements Driver.ScanHandler {
    // A pointer to the the native handler.
    private final long nativeHandler;

    private NativeScanHandler(long nativeHandler) {
        this.nativeHandler = nativeHandler;
    }

    private native void nativeOnDiscovered(
            long nativeHandler, String uuid, Map<String, byte[]> characteristics, int rssi);

    private native void nativeFinalize(long nativeHandler);

    @Override
    public void onDiscovered(String uuid, Map<String, byte[]> characteristics, int rssi) {
        nativeOnDiscovered(nativeHandler, uuid, characteristics, rssi);
    }

    @Override
    protected void finalize() throws Throwable {
        nativeFinalize(nativeHandler);
    }
}
