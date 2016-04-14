// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import io.v.impl.google.naming.NamingUtil;
import io.v.v23.VFutures;
import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.BatchHandle;
import io.v.v23.services.syncbase.RowClient;
import io.v.v23.services.syncbase.RowClientFactory;
import io.v.v23.syncbase.util.Util;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

import java.lang.reflect.Type;

class RowImpl implements Row {
    private final String fullName;
    private final String key;
    private final BatchHandle batchHandle;
    private final RowClient client;

    RowImpl(String parentFullName, String key, BatchHandle batchHandle) {
        // Note, we immediately unescape row keys on the server side. See
        // comment in server/nosql/dispatcher.go for explanation.
        this.fullName = NamingUtil.join(parentFullName, Util.encode(key));
        this.key = key;
        this.batchHandle = batchHandle;
        this.client = RowClientFactory.getRowClient(this.fullName);
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String fullName() {
        return fullName;
    }

    @Override
    public ListenableFuture<Boolean> exists(VContext ctx) {
        return client.exists(ctx, this.batchHandle);
    }

    @Override
    public ListenableFuture<Void> delete(VContext ctx) {
        return client.delete(ctx, this.batchHandle);
    }

    @Override
    public ListenableFuture<Object> get(VContext ctx, final Type type) {
        return VFutures.withUserLandChecks(ctx, Futures.transform(client.get(ctx, this.batchHandle),
                new AsyncFunction<byte[], Object>() {
                    @Override
                    public ListenableFuture<Object> apply(byte[] data) throws Exception {
                        return Futures.immediateFuture(VomUtil.decode(data, type));
                    }
                }));
    }

    @Override
    public ListenableFuture<Void> put(VContext ctx, Object value, Type type) {
        try {
            byte[] data = VomUtil.encode(value, type);
            return client.put(ctx, this.batchHandle, data);
        } catch (VException e) {
            return VFutures.withUserLandChecks(ctx, Futures.<Void>immediateFailedFuture(e));
        }
    }
}
