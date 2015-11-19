// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import io.v.impl.google.naming.NamingUtil;
import io.v.v23.rpc.Callback;
import io.v.v23.services.permissions.ObjectClient;
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
    public boolean exists(VContext ctx) throws VException {
        return client.exists(ctx);
    }
    @Override
    public void exists(VContext ctx, Callback<Boolean> callback) throws VException {
        client.exists(ctx, callback);
    }
    @Override
    public Database getNoSqlDatabase(String relativeName, Schema schema) {
        return NoSql.newDatabase(fullName, relativeName, schema);
    }
    @Override
    public List<String> listDatabases(VContext ctx) throws VException {
        return Util.listChildren(ctx, fullName);
    }
    @Override
    public void listDatabases(VContext ctx, Callback<List<String>> callback) throws VException {
        Util.listChildren(ctx, fullName, callback);
    }
    @Override
    public void create(VContext ctx, Permissions perms) throws VException {
        client.create(ctx, perms);
    }
    @Override
    public void create(VContext ctx, Permissions perms, Callback<Void> callback) throws VException {
        client.create(ctx, perms, callback);
    }
    @Override
    public void destroy(VContext ctx) throws VException {
        client.destroy(ctx);
    }
    @Override
    public void destroy(VContext ctx, Callback<Void> callback) throws VException {
        client.destroy(ctx, callback);
    }
    @Override
    public void setPermissions(VContext ctx, Permissions perms, String version) throws VException {
        client.setPermissions(ctx, perms, version);
    }
    @Override
    public void setPermissions(VContext ctx, Permissions perms, String version,
                               Callback<Void> callback) throws VException {
        client.setPermissions(ctx, perms, version, callback);
    }
    @Override
    public Map<String, Permissions> getPermissions(VContext ctx) throws VException {
        AppClient.GetPermissionsOut perms = client.getPermissions(ctx);
        return ImmutableMap.of(perms.version, perms.perms);
    }
    @Override
    public void getPermissions(VContext ctx, final Callback<Map<String, Permissions>> callback)
            throws VException {
        client.getPermissions(ctx, new Callback<ObjectClient.GetPermissionsOut>() {
            @Override
            public void onSuccess(ObjectClient.GetPermissionsOut result) {
                callback.onSuccess(ImmutableMap.of(result.version, result.perms));
            }

            @Override
            public void onFailure(VException error) {
                callback.onFailure(error);
            }
        });
    }
}
