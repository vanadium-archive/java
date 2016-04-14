// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import com.google.common.util.concurrent.ListenableFuture;
import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.DatabaseClient;
import io.v.v23.services.syncbase.DatabaseClientFactory;
import io.v.v23.services.syncbase.SchemaMetadata;

import javax.annotation.CheckReturnValue;

class SchemaManager {
    private final DatabaseClient client;

    SchemaManager(String dbFullName) {
        client = DatabaseClientFactory.getDatabaseClient(dbFullName);
    }

    @CheckReturnValue
    ListenableFuture<SchemaMetadata> getSchemaMetadata(VContext ctx) {
        return client.getSchemaMetadata(ctx);
    }

    @CheckReturnValue
    ListenableFuture<Void> setSchemaMetadata(VContext ctx, SchemaMetadata metadata) {
        return client.setSchemaMetadata(ctx, metadata);
    }
}
