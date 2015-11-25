// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery;

import java.util.UUID;

/**
 * Stores a {@link UUID} and a {@link ScanHandler}.
 */
public class VScanner {
    private UUID serviceUUID;

    private ScanHandler handler;

    public VScanner(UUID serviceUUID, ScanHandler handler) {
        this.serviceUUID = serviceUUID;
        this.handler = handler;
    }

    /** Returns the {@link UUID} */
    public UUID getServiceUUID() {
        return serviceUUID;
    }

    /** Returns the {@link ScanHandler} */
    public ScanHandler getHandler() {
        return handler;
    }

}
