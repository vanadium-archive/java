// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import java.lang.reflect.Type;
import java.util.List;

import io.v.impl.google.naming.NamingUtil;
import io.v.v23.services.syncbase.nosql.KeyValue;
import io.v.v23.services.syncbase.nosql.PrefixPermissions;
import io.v.v23.services.syncbase.nosql.TableClient;
import io.v.v23.services.syncbase.nosql.TableClientFactory;
import io.v.v23.syncbase.util.Util;
import io.v.v23.context.CancelableVContext;
import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;
import io.v.v23.vdl.TypedClientStream;
import io.v.v23.verror.VException;

class TableImpl implements Table {
    private final String fullName;
    private final String name;
    private final int schemaVersion;
    private final TableClient client;

    TableImpl(String parentFullName, String relativeName, int schemaVersion) {
        this.fullName = NamingUtil.join(parentFullName, relativeName);
        this.name = relativeName;
        this.schemaVersion = schemaVersion;
        this.client = TableClientFactory.getTableClient(this.fullName);
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
        return this.client.exists(ctx, this.schemaVersion);
    }
    @Override
    public Row getRow(String key) {
        return new RowImpl(this.fullName, key, this.schemaVersion);
    }
    @Override
    public Object get(VContext ctx, String key, Type type) throws VException {
        return getRow(key).get(ctx, type);
    }
    @Override
    public void put(VContext ctx, String key, Object value, Type type) throws VException {
        getRow(key).put(ctx, value, type);
    }
    @Override
    public void delete(VContext ctx, RowRange range) throws VException {
        this.client.deleteRowRange(ctx, this.schemaVersion,
                Util.getBytes(range.getStart()), Util.getBytes(range.getLimit()));
    }
    @Override
    public ScanStream scan(VContext ctx, RowRange range) throws VException {
        CancelableVContext ctxC = ctx.withCancel();
        TypedClientStream<Void, KeyValue, Void> stream = this.client.scan(ctxC, this.schemaVersion,
                Util.getBytes(range.getStart()), Util.getBytes(range.getLimit()));
        return new ScanStreamImpl(ctxC, stream);
    }
    @Override
    public PrefixPermissions[] getPermissions(VContext ctx, String key) throws VException {
        List<PrefixPermissions> perms = this.client.getPermissions(ctx, this.schemaVersion, key);
        return perms.toArray(new PrefixPermissions[perms.size()]);
    }
    @Override
    public void setPermissions(VContext ctx, PrefixRange prefix, Permissions perms)
            throws VException {
        this.client.setPermissions(ctx, this.schemaVersion, prefix.getPrefix(), perms);
    }

    @Override
    public void deletePermissions(VContext ctx, PrefixRange prefix) throws VException {
        this.client.deletePermissions(ctx, this.schemaVersion, prefix.getPrefix());
    }
}

