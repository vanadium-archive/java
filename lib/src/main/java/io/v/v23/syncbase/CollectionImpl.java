// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import com.google.common.util.concurrent.ListenableFuture;
import io.v.impl.google.naming.NamingUtil;
import io.v.v23.InputChannel;
import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;
import io.v.v23.services.syncbase.BatchHandle;
import io.v.v23.services.syncbase.CollectionClient;
import io.v.v23.services.syncbase.CollectionClientFactory;
import io.v.v23.services.syncbase.Id;
import io.v.v23.services.syncbase.KeyValue;

import io.v.v23.syncbase.util.Util;

import java.lang.reflect.Type;

class CollectionImpl implements Collection {

    private final String fullName;
    private final Id id;
    private final BatchHandle batchHandle;
    private final CollectionClient client;

    CollectionImpl(String parentFullName, Id id, BatchHandle batchHandle) {
        this.fullName = NamingUtil.join(parentFullName, Util.encodeId(id));
        this.id = id;
        this.batchHandle = batchHandle;
        this.client = CollectionClientFactory.getCollectionClient(fullName);
    }

    @Override
    public Id id() {
        return id;
    }

    @Override
    public String fullName() {
        return fullName;
    }

    @Override
    public ListenableFuture<Void> create(VContext ctx, Permissions perms) {
        return client.create(ctx, this.batchHandle, perms);
    }

    @Override
    public ListenableFuture<Void> destroy(VContext ctx) {
        return client.destroy(ctx, this.batchHandle);
    }

    @Override
    public ListenableFuture<Boolean> exists(VContext ctx) {
        return client.exists(ctx, this.batchHandle);
    }

    @Override
    public ListenableFuture<Permissions> getPermissions(VContext ctx) {
        return client.getPermissions(ctx, this.batchHandle);
    }

    @Override
    public ListenableFuture<Void> setPermissions(VContext ctx, Permissions perms) {
        return client.setPermissions(ctx, this.batchHandle, perms);
    }

    @Override
    public Row getRow(String key) {
        return new RowImpl(fullName, key, this.batchHandle);
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
        return client.deleteRange(ctx, this.batchHandle,
                Util.getBytes(range.getStart()), Util.getBytes(range.getLimit()));
    }

    @Override
    public InputChannel<KeyValue> scan(VContext ctx, RowRange range) {
        return client.scan(ctx, this.batchHandle,
                Util.getBytes(range.getStart()), Util.getBytes(range.getLimit()));
    }
}
