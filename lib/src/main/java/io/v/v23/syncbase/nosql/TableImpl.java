// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.lang.reflect.Type;
import java.util.List;

import io.v.impl.google.naming.NamingUtil;
import io.v.v23.InputChannel;
import io.v.v23.services.syncbase.nosql.KeyValue;
import io.v.v23.services.syncbase.nosql.PrefixPermissions;
import io.v.v23.services.syncbase.nosql.TableClient;
import io.v.v23.services.syncbase.nosql.TableClientFactory;
import io.v.v23.syncbase.util.Util;
import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;
import io.v.v23.vdl.ClientRecvStream;

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
    public ListenableFuture<Void> create(VContext ctx, Permissions perms) {
        return client.create(ctx, schemaVersion, perms);
    }
    @Override
    public ListenableFuture<Void> destroy(VContext ctx) {
        return client.destroy(ctx, schemaVersion);
    }
    @Override
    public ListenableFuture<Boolean> exists(VContext ctx) {
        return client.exists(ctx, schemaVersion);
    }
    @Override
    public ListenableFuture<Permissions> getPermissions(VContext ctx) {
        return client.getPermissions(ctx, schemaVersion);
    }
    @Override
    public ListenableFuture<Void> setPermissions(VContext ctx, Permissions perms) {
        return client.setPermissions(ctx, schemaVersion, perms);
    }
    @Override
    public Row getRow(String key) {
        return new RowImpl(fullName, key, schemaVersion);
    }
    @Override
    public ListenableFuture<Object> get(VContext ctx, String key, Type type) {
        return getRow(key).get(ctx, type);
    }
    @Override
    public ListenableFuture<Void> put(VContext ctx, String key, Object value, Type type) {
        return getRow(key).put(ctx, value, type);
    }
    @Override
    public ListenableFuture<Void> delete(VContext ctx, String key) {
        return getRow(key).delete(ctx);
    }
    @Override
    public ListenableFuture<Void> deleteRange(VContext ctx, RowRange range) {
        return client.deleteRange(ctx, schemaVersion,
                Util.getBytes(range.getStart()), Util.getBytes(range.getLimit()));
    }
    @Override
    public ListenableFuture<InputChannel<KeyValue>> scan(VContext ctx, RowRange range) {
        return Futures.transform(client.scan(ctx, schemaVersion,
                        Util.getBytes(range.getStart()), Util.getBytes(range.getLimit())),
                new Function<ClientRecvStream<KeyValue, Void>, InputChannel<KeyValue>>() {
                    @Override
                    public InputChannel<KeyValue> apply(ClientRecvStream<KeyValue, Void> input) {
                        return input;
                    }
                });
    }
    @Override
    public ListenableFuture<List<PrefixPermissions>> getPrefixPermissions(VContext ctx, String key) {
        return client.getPrefixPermissions(ctx, schemaVersion, key);
    }
    @Override
    public ListenableFuture<Void> setPrefixPermissions(VContext ctx, PrefixRange prefix, Permissions perms)
            {
        return client.setPrefixPermissions(ctx, schemaVersion, prefix.getPrefix(), perms);
    }
    @Override
    public ListenableFuture<Void> deletePrefixPermissions(VContext ctx, PrefixRange prefix) {
        return client.deletePrefixPermissions(ctx, schemaVersion, prefix.getPrefix());
    }
}
