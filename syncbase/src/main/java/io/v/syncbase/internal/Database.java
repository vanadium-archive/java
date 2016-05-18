// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

import java.util.List;

public class Database {
    public static native VersionedPermissions GetPermissions(String name) throws VError;
    public static native void SetPermissions(String name, VersionedPermissions permissions) throws VError;

    public static native void Create(String name, Permissions permissions) throws VError;
    public static native void Destroy(String name) throws VError;
    public static native boolean Exists(String name) throws VError;

    public class BatchOptions {
        String hint;
        boolean readOnly;
    }

    public static native String BeginBatch(String name, BatchOptions options) throws VError;
    public static native List<Id> ListCollections(String name, String batchHandler) throws VError;
    public static native void Commit(String name, String batchHandler) throws VError;
    public static native void Abort(String name, String batchHandler) throws VError;
    public static native byte[] GetResumeMarker(String name, String batchHandler) throws VError;

    public class SyncgroupSpec {
        String description;
        Permissions permissions;
        List<Id> collections;
        List<String> mountTables;
        boolean isPrivate;
    }

    public class VersionedSyncgroupSpec {
        String version;
        SyncgroupSpec syncgroupSpec;
    }

    public class SyncgroupMemberInfo {
        int syncPriority;
        int blobDevType;
    }

    public static native List<Id> ListSyncgroups(String name) throws VError;
    public static native void CreateSyncgroup(String name, Id syncgroupId, SyncgroupSpec spec, SyncgroupMemberInfo info) throws VError;
    public static native SyncgroupSpec JoinSyncgroup(String name, Id syncgroupId, SyncgroupMemberInfo info) throws VError;
    public static native void LeaveSyncgroup(String name, Id syncgroupId) throws VError;
    public static native void DestroySyncgroup(String name, Id syncgroupId) throws VError;
    public static native void EjectFromSyncgroup(String name, Id syncgroupId, String member) throws VError;
    public static native VersionedSyncgroupSpec GetSyncgroupSpec(String name, Id syncgroupId) throws VError;
    public static native void SetSyncgroupSpec(String name, Id syncgroupId, VersionedSyncgroupSpec spec) throws VError;
    public static native List<SyncgroupMemberInfo> GetSyncgroupMembers(String name, Id syncgroupId) throws VError;
}