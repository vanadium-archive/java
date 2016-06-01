// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.discovery.plugins.ble;

import java.util.UUID;

/**
 * Constants for use in the Bluetooth Advertisements.
 */
class Constants {
    // Name and UUID for SDP record.
    static final String SDP_NAME = "v23";
    static final UUID SDP_UUID = UUID.fromString("62e59f86-22b8-572b-82bf-2ee0ea877259");
}
