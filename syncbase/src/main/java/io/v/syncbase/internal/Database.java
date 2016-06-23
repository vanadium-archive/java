// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

import java.util.List;
import java.util.Map;

import io.v.syncbase.core.BatchOptions;
import io.v.syncbase.core.CollectionRowPattern;
import io.v.syncbase.core.Id;
import io.v.syncbase.core.SyncgroupInvite;
import io.v.syncbase.core.Permissions;
import io.v.syncbase.core.SyncgroupMemberInfo;
import io.v.syncbase.core.SyncgroupSpec;
import io.v.syncbase.core.VError;
import io.v.syncbase.core.VersionedPermissions;
import io.v.syncbase.core.VersionedSyncgroupSpec;
import io.v.syncbase.core.WatchChange;

public class Database {
    public static native VersionedPermissions GetPermissions(String name) throws VError;
    public static native void SetPermissions(String name, VersionedPermissions permissions) throws VError;

    public static native void Create(String name, Permissions permissions) throws VError;
    public static native void Destroy(String name) throws VError;
    public static native boolean Exists(String name) throws VError;

    public static native String BeginBatch(String name, BatchOptions options) throws VError;
    public static native List<Id> ListCollections(String name, String batchHandle) throws VError;
    public static native void Commit(String name, String batchHandle) throws VError;
    public static native void Abort(String name, String batchHandle) throws VError;
    public static native byte[] GetResumeMarker(String name, String batchHandle) throws VError;

    public static native List<Id> ListSyncgroups(String name) throws VError;
    public static native void CreateSyncgroup(String name, Id syncgroupId, SyncgroupSpec spec, SyncgroupMemberInfo info) throws VError;
    public static native SyncgroupSpec JoinSyncgroup(String name, String remoteSyncbaseName, List<String> expectedSyncbaseBlessings, Id syncgroupId, SyncgroupMemberInfo info) throws VError;
    public static native void LeaveSyncgroup(String name, Id syncgroupId) throws VError;
    public static native void DestroySyncgroup(String name, Id syncgroupId) throws VError;
    public static native void EjectFromSyncgroup(String name, Id syncgroupId, String member) throws VError;
    public static native VersionedSyncgroupSpec GetSyncgroupSpec(String name, Id syncgroupId) throws VError;
    public static native void SetSyncgroupSpec(String name, Id syncgroupId, VersionedSyncgroupSpec spec) throws VError;
    public static native Map<String, SyncgroupMemberInfo> GetSyncgroupMembers(String name, Id syncgroupId) throws VError;

    public interface WatchPatternsCallbacks {
        void onChange(WatchChange watchChange);
        void onError(VError vError);
    }

    public static native void WatchPatterns(String name, byte[] resumeMarker, List<CollectionRowPattern> patterns, WatchPatternsCallbacks callbacks) throws VError;

    public interface SyncgroupInvitesCallbacks {
        void onInvite(SyncgroupInvite invite);
    }

    public static native long SyncgroupInvitesNewScan(String name, SyncgroupInvitesCallbacks callbacks) throws VError;
    public static native void SyncgroupInvitesStopScan(long id);
}