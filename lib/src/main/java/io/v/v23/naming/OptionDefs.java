// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.naming;

/**
 * Various options in the Vanadium naming package.
 */
public class OptionDefs {
    /**
     * A key for an option of type {@link Boolean} that specifies whether the mount should
     * replace the previous mount.
     */
    public static final String REPLACE_MOUNT = "io.v.v23.naming.REPLACE_MOUNT";

    /**
     * A key for an option of type {@link Boolean} that specifies whether the target is a
     * mount table.
     */
    public static final String SERVES_MOUNT_TABLE = "io.v.v23.naming.SERVES_MOUNT_TABLE";

    /**
     * A key for an option of type {@link Boolean} that specifies whether the target is a leaf.
     */
    public static final String IS_LEAF = "io.v.v23.naming.IS_LEAF";
}
