// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

import java.util.List;

public class Service {
    public static native VersionedPermissions GetPermissions();
    public static native void SetPermissions(VersionedPermissions permissions) throws VError;

    public static native List<Id> ListDatabases() throws Error;
}