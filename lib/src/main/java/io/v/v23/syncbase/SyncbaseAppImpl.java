// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import io.v.impl.google.naming.NamingUtil;
import io.v.v23.services.syncbase.AppClient;
import io.v.v23.services.syncbase.AppClientFactory;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.NoSql;
import io.v.v23.syncbase.nosql.Schema;
import io.v.v23.syncbase.util.Util;
import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;
import io.v.v23.verror.VException;

class SyncbaseAppImpl implements SyncbaseApp {
    private final String fullName;
    private final String name;
    private final AppClient client;

    SyncbaseAppImpl(String parentFullName, String relativeName) {
        this.fullName = NamingUtil.join(parentFullName, relativeName);
        this.name = relativeName;
        this.client = AppClientFactory.getAppClient(this.fullName);
    }

    @Override
    public String name() {
        return this.name;
    }
    @Override
    public String fullName() {
        return this.fullName;
    }
    @Override
    public boolean exists(VContext ctx) throws VException {
        return this.client.exists(ctx);
    }
    @Override
    public Database getNoSqlDatabase(String relativeName, Schema schema) {
        return NoSql.newDatabase(this.fullName, relativeName, schema);
    }
    @Override
    public String[] listDatabases(VContext ctx) throws VException {
        List<String> x = this.client.listDatabases(ctx);
        return x.toArray(new String[x.size()]);
    }
    @Override
    public void create(VContext ctx, Permissions perms) throws VException {
        this.client.create(ctx, perms);
    }
    @Override
    public void destroy(VContext ctx) throws VException {
        this.client.destroy(ctx);
    }
    @Override
    public void setPermissions(VContext ctx, Permissions perms, String version) throws VException {
        this.client.setPermissions(ctx, perms, version);
    }
    @Override
    public Map<String, Permissions> getPermissions(VContext ctx) throws VException {
        AppClient.GetPermissionsOut perms = this.client.getPermissions(ctx);
        return ImmutableMap.of(perms.version, perms.perms);
    }
}
