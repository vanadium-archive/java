// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery;

import java.util.List;

import io.v.v23.context.VContext;
import io.v.v23.discovery.Service;
import io.v.v23.discovery.VDiscovery;
import io.v.v23.security.BlessingPattern;

/**
 * Implements the {@link VDiscovery} interface.  The VDiscovery interface allows a vanadium service
 * to advertise itself and clients to scan for these advertisements.
 */
public class VDiscoveryImpl implements VDiscovery {
    private long nativeDiscovery;
    private long nativeTrigger;

    private native void nativeFinalize(long discovery, long trigger);

    private VDiscoveryImpl(long nativeDiscovery, long nativeTrigger) {
        this.nativeDiscovery = nativeDiscovery;
        this.nativeTrigger = nativeTrigger;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        nativeFinalize(nativeDiscovery, nativeTrigger);
    }

    @Override
    public native void advertise(VContext ctx, Service service, List<BlessingPattern> patterns,
                                 AdvertiseDoneCallback cb);

    @Override
    public native void scan(VContext ctx, String query, ScanCallback scanCallback);
}
