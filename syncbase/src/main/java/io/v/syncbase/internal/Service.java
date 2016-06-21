// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

import java.util.List;

import io.v.syncbase.core.Id;
import io.v.syncbase.core.VError;
import io.v.syncbase.core.VersionedPermissions;

public class Service {
    public static native void Init(String rootDir, boolean testLogin);
    public static native void Serve() throws VError;
    public static native void Shutdown();

    public static native VersionedPermissions GetPermissions();
    public static native void SetPermissions(VersionedPermissions permissions) throws VError;

    public static native List<Id> ListDatabases() throws Error;
    public static native void Login(String oAuthProvider, String oAuthToken) throws VError;
    public static native boolean IsLoggedIn();
}