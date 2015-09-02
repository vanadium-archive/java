// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.nosql.DatabaseClient;
import io.v.v23.services.syncbase.nosql.DatabaseClientFactory;
import io.v.v23.services.syncbase.nosql.SchemaMetadata;
import io.v.v23.verror.VException;

class SchemaManager {
    private final DatabaseClient client;

    SchemaManager(String dbFullName) {
        this.client = DatabaseClientFactory.getDatabaseClient(dbFullName);
    }

    SchemaMetadata getSchemaMetadata(VContext ctx) throws VException {
        return this.client.getSchemaMetadata(ctx);
    }

    void setSchemaMetadata(VContext ctx, SchemaMetadata metadata) throws VException {
        this.client.setSchemaMetadata(ctx, metadata);
    }
}