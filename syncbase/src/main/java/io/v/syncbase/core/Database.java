// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

import java.util.List;

public class Database extends DatabaseHandle {

    protected Database(Id id) {
        super(id);
    }

    public VersionedPermissions getPermissions() throws VError {
        return io.v.syncbase.internal.Database.GetPermissions(fullName);
    }

    public void setPermissions(VersionedPermissions permissions) throws VError {
        io.v.syncbase.internal.Database.SetPermissions(fullName, permissions);
    }

    public void create(Permissions permissions) throws VError {
        io.v.syncbase.internal.Database.Create(fullName, permissions);
    }

    public void destroy() throws VError {
        io.v.syncbase.internal.Database.Destroy(fullName);
    }

    public boolean exists() throws VError {
        return io.v.syncbase.internal.Database.Exists(fullName);
    }

    public BatchDatabase beginBatch(BatchOptions options) throws VError {
        String batchHandle = io.v.syncbase.internal.Database.BeginBatch(fullName, options);
        return new BatchDatabase(this.id, batchHandle);
    }

    public Syncgroup syncgroup(String name) throws VError {
        return syncgroup(new Id(io.v.syncbase.internal.Blessings.UserBlessingFromContext(), name));
    }

    public Syncgroup syncgroup(Id id) {
        return new Syncgroup(this, id);
    }

    public List<Id> listSyncgroups() throws VError {
        return io.v.syncbase.internal.Database.ListSyncgroups(fullName);
    }

    public interface WatchPatternsCallbacks {
        void onChange(WatchChange watchChange);
        void onError(VError vError);
    }

    public void watch(byte[] resumeMarker, List<CollectionRowPattern> patterns,
                      WatchPatternsCallbacks callbacks) throws VError {}
}