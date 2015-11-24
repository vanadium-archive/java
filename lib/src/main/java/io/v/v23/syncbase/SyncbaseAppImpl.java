// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import io.v.impl.google.naming.NamingUtil;
import io.v.v23.services.syncbase.AppClient;
import io.v.v23.services.syncbase.AppClientFactory;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.NoSql;
import io.v.v23.syncbase.nosql.Schema;
import io.v.v23.syncbase.util.Util;
import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;

class SyncbaseAppImpl implements SyncbaseApp {
    private final String fullName;
    private final String name;
    private final AppClient client;

    SyncbaseAppImpl(String parentFullName, String relativeName) {
        this.fullName = NamingUtil.join(parentFullName, Util.escape(relativeName));
        this.name = relativeName;
        this.client = AppClientFactory.getAppClient(fullName);
    }

    @Override
    public String name() {
        return name;
    }
    @Override
    public String fullName() {
        return fullName;
    }
    @Override
    public ListenableFuture<Boolean> exists(VContext ctx) {
        return client.exists(ctx);
    }
    @Override
    public Database getNoSqlDatabase(String relativeName, Schema schema) {
        return NoSql.newDatabase(fullName, relativeName, schema);
    }
    @Override
    public ListenableFuture<List<String>> listDatabases(VContext ctx) {
        return Util.listChildren(ctx, fullName);
    }
    @Override
    public ListenableFuture<Void> create(VContext ctx, Permissions perms) {
        return client.create(ctx, perms);
    }
    @Override
    public ListenableFuture<Void> destroy(VContext ctx) {
        return client.destroy(ctx);
    }
    @Override
    public ListenableFuture<Void> setPermissions(VContext ctx, Permissions perms, String version) {
        return client.setPermissions(ctx, perms, version);
    }
    @Override
    public ListenableFuture<Map<String, Permissions>> getPermissions(VContext ctx) {
        return Futures.transform(client.getPermissions(ctx),
                new Function<AppClient.GetPermissionsOut, Map<String, Permissions>>() {
                    @Override
                    public Map<String, Permissions> apply(AppClient.GetPermissionsOut perms) {
                        return ImmutableMap.of(perms.version, perms.perms);
                    }
                });
    }
}
