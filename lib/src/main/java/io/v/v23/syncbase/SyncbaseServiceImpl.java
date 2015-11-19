// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import io.v.v23.rpc.Callback;
import io.v.v23.services.permissions.ObjectClient;
import io.v.v23.services.syncbase.ServiceClient;
import io.v.v23.services.syncbase.ServiceClientFactory;
import io.v.v23.syncbase.util.Util;
import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;
import io.v.v23.verror.VException;

class SyncbaseServiceImpl implements SyncbaseService {
    private final String fullName;
    private final ServiceClient client;

    SyncbaseServiceImpl(String fullName) {
        this.fullName = fullName;
        this.client = ServiceClientFactory.getServiceClient(fullName);
    }

    @Override
    public String fullName() {
        return fullName;
    }
    @Override
    public SyncbaseApp getApp(String relativeName) {
        return new SyncbaseAppImpl(fullName, relativeName);
    }
    @Override
    public List<String> listApps(VContext ctx) throws VException {
        return Util.listChildren(ctx, fullName);
    }
    @Override
    public void listApps(VContext ctx, Callback<List<String>> callback) throws VException {
        Util.listChildren(ctx, fullName, callback);
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
        ServiceClient.GetPermissionsOut perms = client.getPermissions(ctx);
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
