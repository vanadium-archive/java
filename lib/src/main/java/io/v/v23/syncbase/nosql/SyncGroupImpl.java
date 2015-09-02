// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

import io.v.v23.services.syncbase.nosql.DatabaseClient;
import io.v.v23.services.syncbase.nosql.DatabaseClientFactory;
import io.v.v23.services.syncbase.nosql.SyncGroupMemberInfo;
import io.v.v23.services.syncbase.nosql.SyncGroupSpec;
import io.v.v23.context.VContext;
import io.v.v23.verror.VException;

class SyncGroupImpl implements SyncGroup {
    private final String name;
    private final String dbFullName;
    private final DatabaseClient dbClient;

    SyncGroupImpl(String dbFullName, String name) {
        this.name = name;
        this.dbFullName = dbFullName;
        this.dbClient = DatabaseClientFactory.getDatabaseClient(dbFullName);
    }
    @Override
    public void create(VContext ctx, SyncGroupSpec spec, SyncGroupMemberInfo info) throws VException {
        this.dbClient.createSyncGroup(ctx, this.name, spec, info);
    }
    @Override
    public SyncGroupSpec join(VContext ctx, SyncGroupMemberInfo info) throws VException {
        return this.dbClient.joinSyncGroup(ctx, this.name, info);
    }
    @Override
    public void leave(VContext ctx) throws VException {
        this.dbClient.leaveSyncGroup(ctx, this.name);
    }
    @Override
    public void destroy(VContext ctx) throws VException {
        this.dbClient.destroySyncGroup(ctx, this.name);
    }
    @Override
    public void eject(VContext ctx, String member) throws VException {
        this.dbClient.ejectFromSyncGroup(ctx, this.name, member);
    }
    @Override
    public Map<String, SyncGroupSpec> getSpec(VContext ctx) throws VException {
        DatabaseClient.GetSyncGroupSpecOut spec = this.dbClient.getSyncGroupSpec(ctx, this.name);
        return ImmutableMap.of(spec.version, spec.spec);
    }
    @Override
    public void setSpec(VContext ctx, SyncGroupSpec spec, String version) throws VException {
        this.dbClient.setSyncGroupSpec(ctx, this.name, spec, version);
    }
    @Override
    public Map<String, SyncGroupMemberInfo> getMembers(VContext ctx) throws VException {
        return this.dbClient.getSyncGroupMembers(ctx, this.name);
    }
}