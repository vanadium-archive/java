// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

import java.util.List;

public class Service {
    public static VersionedPermissions getPermissions() {
        return io.v.syncbase.internal.Service.GetPermissions();
    }

    public static void setPermissions(VersionedPermissions permissions) throws VError {
        io.v.syncbase.internal.Service.SetPermissions(permissions);
    }

    public static Database database(String name) throws VError {
        return database(new Id(io.v.syncbase.internal.Blessings.AppBlessingFromContext(), name));
    }

    public static Database database(Id id) {
        return new Database(id);
    }

    public static List<Id> listDatabases() throws Error {
        return io.v.syncbase.internal.Service.ListDatabases();
    }
}