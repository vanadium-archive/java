// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import io.v.v23.services.syncbase.nosql.DatabaseClient;
import io.v.v23.services.syncbase.nosql.DatabaseClientFactory;
import io.v.v23.services.syncbase.nosql.SyncgroupMemberInfo;
import io.v.v23.services.syncbase.nosql.SyncgroupSpec;
import io.v.v23.context.VContext;
import io.v.v23.verror.VException;

class SyncgroupImpl implements Syncgroup {
    private final String name;
    private final String dbFullName;
    private final DatabaseClient dbClient;

    SyncgroupImpl(String dbFullName, String name) {
        this.name = name;
        this.dbFullName = dbFullName;
        this.dbClient = DatabaseClientFactory.getDatabaseClient(dbFullName);
    }
    @Override
    public void create(VContext ctx, SyncgroupSpec spec, SyncgroupMemberInfo info) throws VException {
        this.dbClient.createSyncgroup(ctx, this.name, spec, info);
    }
    @Override
    public SyncgroupSpec join(VContext ctx, SyncgroupMemberInfo info) throws VException {
        return this.dbClient.joinSyncgroup(ctx, this.name, info);
    }
    @Override
    public void leave(VContext ctx) throws VException {
        this.dbClient.leaveSyncgroup(ctx, this.name);
    }
    @Override
    public void destroy(VContext ctx) throws VException {
        this.dbClient.destroySyncgroup(ctx, this.name);
    }
    @Override
    public void eject(VContext ctx, String member) throws VException {
        this.dbClient.ejectFromSyncgroup(ctx, this.name, member);
    }
    @Override
    public Map<String, SyncgroupSpec> getSpec(VContext ctx) throws VException {
        DatabaseClient.GetSyncgroupSpecOut spec = this.dbClient.getSyncgroupSpec(ctx, this.name);
        return ImmutableMap.of(spec.version, spec.spec);
    }
    @Override
    public void setSpec(VContext ctx, SyncgroupSpec spec, String version) throws VException {
        this.dbClient.setSyncgroupSpec(ctx, this.name, spec, version);
    }
    @Override
    public Map<String, SyncgroupMemberInfo> getMembers(VContext ctx) throws VException {
        return this.dbClient.getSyncgroupMembers(ctx, this.name);
    }
}