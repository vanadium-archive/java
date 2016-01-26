// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery;

/**
 * Stores an interfance name and a {@link ScanHandler}.
 */
public class VScanner {
    private String interfaceName;
    private ScanHandler handler;

    public VScanner(String interfaceName, ScanHandler handler) {
        this.interfaceName = interfaceName;
        this.handler = handler;
    }

    /** Returns the interface name */
    public String getInterfaceName() {
        return interfaceName;
    }

    /** Returns the {@link ScanHandler} */
    public ScanHandler getHandler() {
        return handler;
    }
}
