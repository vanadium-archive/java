// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery;

import java.util.UUID;

/**
 * Utility functions for generating UUIDs from interface names and attribute names.
 */
public class UUIDUtil {
    /*
     * Returns a version 5 UUID for the given interface name.
     */
    public static native UUID serviceUUID(String interfaceName);

    /*
     * returns a version 5 UUID for the given attribute name.
     */
    public static native UUID attributeUUID(String name);
}
