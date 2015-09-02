// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

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
        return this.fullName;
    }
    @Override
    public SyncbaseApp getApp(String relativeName) {
        return new SyncbaseAppImpl(this.fullName, relativeName);
    }
    @Override
    public String[] listApps(VContext ctx) throws VException {
        return Util.list(ctx, this.fullName);
    }
    @Override
    public void setPermissions(VContext ctx, Permissions perms, String version) throws VException {
        this.client.setPermissions(ctx, perms, version);
    }
    @Override
    public Map<String, Permissions> getPermissions(VContext ctx) throws VException {
        ServiceClient.GetPermissionsOut perms = this.client.getPermissions(ctx);
        return ImmutableMap.of(perms.version, perms.perms);
    }
}