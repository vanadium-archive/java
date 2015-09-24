// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import java.lang.reflect.Type;

import io.v.impl.google.naming.NamingUtil;
import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.nosql.RowClient;
import io.v.v23.services.syncbase.nosql.RowClientFactory;
import io.v.v23.syncbase.util.Util;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

class RowImpl implements Row {
    private final String fullName;
    private final String key;
    private final int schemaVersion;
    private final RowClient client;

    RowImpl(String parentFullName, String key, int schemaVersion) {
        // Note, we immediately unescape row keys on the server side. See
        // comment in server/nosql/dispatcher.go for explanation.
        this.fullName = NamingUtil.join(parentFullName, Util.escape(key));
        this.key = key;
        this.schemaVersion = schemaVersion;
        this.client = RowClientFactory.getRowClient(this.fullName);
    }
    @Override
    public String key() {
        return this.key;
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
    public void delete(VContext ctx) throws VException {
        this.client.delete(ctx, this.schemaVersion);
    }
    @Override
    public Object get(VContext ctx, Type type) throws VException {
        byte[] data = this.client.get(ctx, this.schemaVersion);
        return VomUtil.decode(data, type);
    }
    @Override
    public void put(VContext ctx, Object value, Type type) throws VException {
        byte[] data = VomUtil.encode(value, type);
        this.client.put(ctx, this.schemaVersion, data);
    }
}
