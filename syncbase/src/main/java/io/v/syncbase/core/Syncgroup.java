// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

import java.util.List;
import java.util.Map;

public class Syncgroup {
    private final String dbFullName;
    private final Id id;

    Syncgroup(Database database, Id id) {
        dbFullName = database.fullName;
        this.id = id;
    }

    public Id getId() {
        return id;
    }

    public void create(SyncgroupSpec spec, SyncgroupMemberInfo info) throws VError {
        io.v.syncbase.internal.Database.CreateSyncgroup(dbFullName, id, spec, info);
    }

    public SyncgroupSpec join(String remoteSyncbaseName, List<String> expectedSyncbaseBlessings,
                              SyncgroupMemberInfo info)
            throws VError {
        return io.v.syncbase.internal.Database.JoinSyncgroup(
                dbFullName, remoteSyncbaseName, expectedSyncbaseBlessings, id, info);
    }

    public void leave() throws VError {
        io.v.syncbase.internal.Database.LeaveSyncgroup(dbFullName, id);
    }

    public void destroy() throws VError {
        io.v.syncbase.internal.Database.DestroySyncgroup(dbFullName, id);
    }

    public void eject(String member) throws VError {
        io.v.syncbase.internal.Database.EjectFromSyncgroup(dbFullName, id, member);
    }

    public VersionedSyncgroupSpec getSpec() throws VError {
        return io.v.syncbase.internal.Database.GetSyncgroupSpec(dbFullName, id);
    }

    public void setSpec(VersionedSyncgroupSpec spec) throws VError {
        io.v.syncbase.internal.Database.SetSyncgroupSpec(dbFullName, id, spec);
    }

    public Map<String, SyncgroupMemberInfo> getMembers() throws VError {
        return io.v.syncbase.internal.Database.GetSyncgroupMembers(dbFullName, id);
    }
}
