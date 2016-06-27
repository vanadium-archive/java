// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

public class TestConstants {
    public static Permissions anyDbPermissions() {
        return new Permissions("{\"Admin\":{\"In\":[\"...\"]},\"Write\":{\"In\":[\"...\"]},\"Read\":{\"In\":[\"...\"]},\"Resolve\":{\"In\":[\"...\"]}}".getBytes());

    }

    public static Permissions anyCollectionPermissions() {
        return new Permissions("{\"Admin\":{\"In\":[\"...\"]},\"Write\":{\"In\":[\"...\"]},\"Read\":{\"In\":[\"...\"]}}".getBytes());
    }

    public static Permissions anySyncgroupPermissions() {
        return new Permissions("{\"Admin\":{\"In\":[\"...\"],\"NotIn\":null},\"Read\":{\"In\":[\"...\"],\"NotIn\":null}}".getBytes());
    }
}
