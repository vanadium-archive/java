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
import io.v.v23.vdl.VdlAny;
import io.v.v23.vom.VomUtil;

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
    public <T> ListenableFuture<T> get(VContext ctx, final Class<T> clazz) {
        return VFutures.withUserLandChecks(ctx, Futures.transform(client.get(ctx, this.batchHandle),
                new AsyncFunction<VdlAny, T>() {
                    @Override
                    public ListenableFuture<T> apply(VdlAny vdlAny) throws Exception {
                        final byte[] encodedBytes = VomUtil.encode(vdlAny, VdlAny.VDL_TYPE);
                        final Object decodedObject = VomUtil.decode(encodedBytes, clazz);
                        return Futures.immediateFuture((T) decodedObject);
                    }
                }));
    }

    @Override
    public ListenableFuture<Void> put(VContext ctx, Object value) {
        return client.put(ctx, this.batchHandle, new VdlAny(value.getClass(), value));
    }
}
