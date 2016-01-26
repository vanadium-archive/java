// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.lib.discovery;

import java.util.UUID;

/**
 * Utility functions for generating UUIDs from interface names and attribute keys.
 */
public class UUIDUtil {
    public static native UUID UUIDForInterfaceName(String name);
    public static native UUID UUIDForAttributeKey(String key);
}
