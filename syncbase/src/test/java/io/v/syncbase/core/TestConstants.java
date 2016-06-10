// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

public class TestConstants {
    public static Permissions anyDbPermissions() {
        Permissions permissions = new Permissions();
        permissions.json = "{\"Admin\":{\"In\":[\"...\"]},\"Write\":{\"In\":[\"...\"]},\"Read\":{\"In\":[\"...\"]},\"Resolve\":{\"In\":[\"...\"]}}".getBytes();
        return permissions;
    }

    public static Permissions anyCollectionPermissions() {
        Permissions permissions = new Permissions();
        permissions.json = "{\"Admin\":{\"In\":[\"...\"]},\"Write\":{\"In\":[\"...\"]},\"Read\":{\"In\":[\"...\"]}}".getBytes();
        return permissions;
    }

    public static Permissions anySyncgroupPermissions() {
        Permissions permissions = new Permissions();
        // The '"NotIn":null' are present to make easier the comparison with what Syncbase returns.
        permissions.json = "{\"Admin\":{\"In\":[\"...\"],\"NotIn\":null},\"Read\":{\"In\":[\"...\"],\"NotIn\":null}}".getBytes();
        return permissions;
    }
}
