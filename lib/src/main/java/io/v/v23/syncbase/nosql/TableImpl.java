// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import java.lang.reflect.Type;
import java.util.List;

import io.v.impl.google.naming.NamingUtil;
import io.v.v23.VIterable;
import io.v.v23.services.syncbase.nosql.KeyValue;
import io.v.v23.services.syncbase.nosql.PrefixPermissions;
import io.v.v23.services.syncbase.nosql.TableClient;
import io.v.v23.services.syncbase.nosql.TableClientFactory;
import io.v.v23.syncbase.util.Util;
import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;
import io.v.v23.verror.VException;

class TableImpl implements Table {
    private final String fullName;
    private final String name;
    private final int schemaVersion;
    private final TableClient client;

    TableImpl(String parentFullName, String relativeName, int schemaVersion) {
        // Escape relativeName so that any forward slashes get dropped, thus
        // ensuring that the server will interpret fullName as referring to a
        // table object. Note that the server will still reject this name if
        // util.ValidTableName returns false.
        this.fullName = NamingUtil.join(parentFullName, Util.escape(relativeName));
        this.name = relativeName;
        this.schemaVersion = schemaVersion;
        this.client = TableClientFactory.getTableClient(fullName);
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
    public void create(VContext ctx, Permissions perms) throws VException {
        client.create(ctx, schemaVersion, perms);
    }
    @Override
    public void destroy(VContext ctx) throws VException {
        client.destroy(ctx, schemaVersion);
    }
    @Override
    public boolean exists(VContext ctx) throws VException {
        return client.exists(ctx, schemaVersion);
    }
    @Override
    public Permissions getPermissions(VContext ctx) throws VException {
        return client.getPermissions(ctx, schemaVersion);
    }
    @Override
    public void setPermissions(VContext ctx, Permissions perms) throws VException {
        client.setPermissions(ctx, schemaVersion, perms);
    }
    @Override
    public Row getRow(String key) {
        return new RowImpl(fullName, key, schemaVersion);
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
    public void delete(VContext ctx, String key) throws VException {
        getRow(key).delete(ctx);
    }
    @Override
    public void deleteRange(VContext ctx, RowRange range) throws VException {
        client.deleteRange(ctx, schemaVersion,
                Util.getBytes(range.getStart()), Util.getBytes(range.getLimit()));
    }
    @Override
    public VIterable<KeyValue> scan(VContext ctx, RowRange range) throws VException {
        return client.scan(ctx, schemaVersion,
                Util.getBytes(range.getStart()), Util.getBytes(range.getLimit()));
    }
    @Override
    public PrefixPermissions[] getPrefixPermissions(VContext ctx, String key) throws VException {
        List<PrefixPermissions> perms = client.getPrefixPermissions(
                ctx, schemaVersion, key);
        return perms.toArray(new PrefixPermissions[perms.size()]);
    }
    @Override
    public void setPrefixPermissions(VContext ctx, PrefixRange prefix, Permissions perms)
            throws VException {
        client.setPrefixPermissions(ctx, schemaVersion, prefix.getPrefix(), perms);
    }
    @Override
    public void deletePrefixPermissions(VContext ctx, PrefixRange prefix) throws VException {
        client.deletePrefixPermissions(ctx, schemaVersion, prefix.getPrefix());
    }
}
