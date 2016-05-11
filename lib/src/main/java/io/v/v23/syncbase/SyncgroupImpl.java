// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.v.v23.VFutures;
import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.DatabaseClient;
import io.v.v23.services.syncbase.DatabaseClientFactory;
import io.v.v23.services.syncbase.Id;
import io.v.v23.services.syncbase.SyncgroupManagerClient;
import io.v.v23.services.syncbase.SyncgroupMemberInfo;
import io.v.v23.services.syncbase.SyncgroupSpec;

import java.util.List;
import java.util.Map;

class SyncgroupImpl implements Syncgroup {
    private final Id id;
    private final String dbFullName;
    private final DatabaseClient dbClient;

    SyncgroupImpl(String dbFullName, Id id) {
        this.id = id;
        this.dbFullName = dbFullName;
        this.dbClient = DatabaseClientFactory.getDatabaseClient(dbFullName);
    }

    @Override
    public ListenableFuture<Void> create(VContext ctx, SyncgroupSpec spec,
                                         SyncgroupMemberInfo info) {
        return dbClient.createSyncgroup(ctx, id, spec, info);
    }

    @Override
    public ListenableFuture<SyncgroupSpec> join(VContext ctx, String syncbaseName, List<String> expectedSyncbaseBlessings, SyncgroupMemberInfo info) {
        return dbClient.joinSyncgroup(ctx, syncbaseName, expectedSyncbaseBlessings, id, info);
    }

    @Override
    public ListenableFuture<Void> leave(VContext ctx) {
        return dbClient.leaveSyncgroup(ctx, id);
    }

    @Override
    public ListenableFuture<Void> destroy(VContext ctx) {
        return dbClient.destroySyncgroup(ctx, id);
    }

    @Override
    public ListenableFuture<Void> eject(VContext ctx, String member) {
        return dbClient.ejectFromSyncgroup(ctx, id, member);
    }

    @Override
    public ListenableFuture<Map<String, SyncgroupSpec>> getSpec(VContext ctx) {
        return VFutures.withUserLandChecks(ctx,
                Futures.transform(dbClient.getSyncgroupSpec(ctx, id), new Function<
                        SyncgroupManagerClient.GetSyncgroupSpecOut, Map<String, SyncgroupSpec>>() {
                    @Override
                    public Map<String, SyncgroupSpec> apply(
                            SyncgroupManagerClient.GetSyncgroupSpecOut spec) {
                        return ImmutableMap.of(spec.version, spec.spec);
                    }
                }));
    }

    @Override
    public ListenableFuture<Void> setSpec(VContext ctx, SyncgroupSpec spec, String version) {
        return dbClient.setSyncgroupSpec(ctx, id, spec, version);
    }

    @Override
    public ListenableFuture<Map<String, SyncgroupMemberInfo>> getMembers(VContext ctx) {
        return dbClient.getSyncgroupMembers(ctx, id);
    }
}
