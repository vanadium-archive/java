// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.discovery.plugins;

import io.v.x.ref.lib.discovery.AdInfo;

import io.v.impl.google.lib.discovery.Plugin;

/**
 * An implementation of the {@link Plugin.ScanHandler} for use by the discovery framework.
 * <p>
 * This handler is used to pass results from a Java plugin to the Go wrapper to passed
 * on to the discovery instance.
 */
class NativeScanHandler implements Plugin.ScanHandler {
    // A pointer to the the native channel.
    private final long nativeChan;

    private NativeScanHandler(long nativeChan) {
        this.nativeChan = nativeChan;
    }

    private native void nativeHandleUpdate(long chan, AdInfo adinfo);

    private native void nativeFinalize(long chan);

    @Override
    public void handleUpdate(AdInfo adinfo) {
        nativeHandleUpdate(nativeChan, adinfo);
    }

    @Override
    protected void finalize() throws Throwable {
        nativeFinalize(nativeChan);
    }
}
