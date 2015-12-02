// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import java.util.UUID;

/**
 * Constants shared across the activities.
 */
public class Constants {
    // Unique id used for establishing all Bluetooth connections.
    public static final UUID MY_UUID_SECURE =
            UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    public static final String ACCOUNT_TYPE = "io.vanadium";

    // For activities that are started for result.
    public static final String REPLY = "REPLY";
    public static final String ERROR = "ERROR";

    public static final String IDENTITY_DEV_V_IO_U_GOOGLE = "identity/dev.v.io:u/google";
}
