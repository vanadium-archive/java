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

import io.v.v23.services.syncbase.ServiceClient;
import io.v.v23.services.syncbase.ServiceClientFactory;
import io.v.v23.syncbase.util.Util;
import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;

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
    public ListenableFuture<List<String>> listApps(VContext ctx) {
        return Util.listChildren(ctx, fullName);
    }
    @Override
    public ListenableFuture<Void> setPermissions(VContext ctx, Permissions perms, String version) {
        return client.setPermissions(ctx, perms, version);
    }
    @Override
    public ListenableFuture<Map<String, Permissions>> getPermissions(VContext ctx) {
        ListenableFuture<ServiceClient.GetPermissionsOut> perms = client.getPermissions(ctx);
        return Futures.transform(perms, new Function<ServiceClient.GetPermissionsOut,
                Map<String, Permissions>>() {
            @Override
            public Map<String, Permissions> apply(ServiceClient.GetPermissionsOut perms) {
                return ImmutableMap.of(perms.version, perms.perms);
            }
        });
    }
}
