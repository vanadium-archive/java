// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import java.io.EOFException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import io.v.impl.google.naming.NamingUtil;
import io.v.v23.services.syncbase.nosql.BatchOptions;
import io.v.v23.services.syncbase.nosql.DatabaseClient;
import io.v.v23.services.syncbase.nosql.DatabaseClientFactory;
import io.v.v23.services.syncbase.nosql.SchemaMetadata;
import io.v.v23.services.syncbase.nosql.TableClient;
import io.v.v23.services.syncbase.nosql.TableClientFactory;
import io.v.v23.syncbase.util.Util;
import io.v.v23.context.CancelableVContext;
import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;
import io.v.v23.vdl.TypedClientStream;
import io.v.v23.vdl.Types;
import io.v.v23.vdl.VdlAny;
import io.v.v23.vdl.VdlOptional;
import io.v.v23.verror.VException;

class DatabaseImpl implements Database, BatchDatabase {
    private final String parentFullName;
    private final String fullName;
    private final String name;
    private final Schema schema;
    private final DatabaseClient client;

    DatabaseImpl(String parentFullName, String relativeName, Schema schema) {
        this.parentFullName = parentFullName;
        this.fullName = NamingUtil.join(parentFullName, relativeName);
        this.name = relativeName;
        this.schema = schema;
        this.client = DatabaseClientFactory.getDatabaseClient(this.fullName);
    }

    // Implements DatabaseCore interface.
    @Override
    public String name() {
        return this.name;
    }
    @Override
    public String fullName() {
        return this.fullName;
    }
    @Override
    public Table getTable(String relativeName) {
        return new TableImpl(this.fullName, relativeName, getSchemaVersion());
    }
    @Override
    public String[] listTables(VContext ctx) throws VException {
        return Util.list(ctx, this.fullName);
    }
    @Override
    public ResultStream exec(VContext ctx, String query) throws VException {
        CancelableVContext ctxC = ctx.withCancel();
        TypedClientStream<Void, List<VdlAny>, Void> stream =
                this.client.exec(ctxC, getSchemaVersion(), query);

        // The first row contains column names, pull them off the stream.
        List<VdlAny> row = null;
        try {
            row = stream.recv();
        } catch (EOFException e) {
            throw new VException("Got empty exec() stream for query: " + query);
        }
        String[] columnNames = new String[row.size()];
        for (int i = 0; i < row.size(); ++i) {
            Serializable elem = row.get(i).getElem();
            if (elem instanceof String) {
                columnNames[i] = (String) elem;
            } else {
                throw new VException("Expected first row in exec() stream to contain column " +
                        "names (of type String), got type: " + elem.getClass());
            }
        }
        return new ResultStreamImpl(ctxC, stream, Arrays.asList(columnNames));
    }

    // Implements AccessController interface.
    @Override
    public void setPermissions(VContext ctx, Permissions perms, String version) throws VException {
        this.client.setPermissions(ctx, perms, version);
    }
    @Override
    public Map<String, Permissions> getPermissions(VContext ctx) throws VException {
        DatabaseClient.GetPermissionsOut perms = this.client.getPermissions(ctx);
        return ImmutableMap.of(perms.version, perms.perms);
    }

    // Implements Database interface.
    @Override
    public boolean exists(VContext ctx) throws VException {
        return this.client.exists(ctx, getSchemaVersion());
    }
    @Override
    public void create(VContext ctx, Permissions perms) throws VException {
        VdlOptional metadataOpt = this.schema != null
                ? VdlOptional.of(this.schema.getMetadata())
                : new VdlOptional<SchemaMetadata>(Types.optionalOf(SchemaMetadata.VDL_TYPE));
        this.client.create(ctx, metadataOpt, perms);
    }
    @Override
    public void delete(VContext ctx) throws VException {
        this.client.delete(ctx, getSchemaVersion());
    }
    @Override
    public void createTable(VContext ctx, String relativeName, Permissions perms)
            throws VException {
        String tableFullName = NamingUtil.join(this.fullName, relativeName);
        TableClient table = TableClientFactory.getTableClient(tableFullName);
        table.create(ctx, getSchemaVersion(), perms);
    }
    @Override
    public void deleteTable(VContext ctx, String relativeName) throws VException {
        String tableFullName = NamingUtil.join(this.fullName, relativeName);
        TableClient table = TableClientFactory.getTableClient(tableFullName);
        table.delete(ctx, getSchemaVersion());
    }
    @Override
    public BatchDatabase beginBatch(VContext ctx, BatchOptions opts) throws VException {
        String relativeName = this.client.beginBatch(ctx, getSchemaVersion(), opts);
        return new DatabaseImpl(this.parentFullName, relativeName, this.schema);
    }
    @Override
    public SyncGroup getSyncGroup(String name) {
        return new SyncGroupImpl(this.fullName, name);
    }
    @Override
    public String[] listSyncGroupNames(VContext ctx) throws VException {
        List<String> names = this.client.getSyncGroupNames(ctx);
        return names.toArray(new String[names.size()]);
    }
    @Override
    public boolean upgradeIfOutdated(VContext ctx) throws VException {
        if (this.schema == null) {
            throw new VException(io.v.v23.flow.Errors.BAD_STATE, ctx,
                    "Schema or SchemaMetadata cannot be null. A valid Schema needs to be used " +
                    "when creating a database handle.");
        }
        if (this.schema.getMetadata().getVersion() < 0) {
            throw new VException(io.v.v23.flow.Errors.BAD_STATE, ctx,
                    "Schema version cannot be less than zero.");
        }
        SchemaManager schemaManager = new SchemaManager(this.fullName);
        SchemaMetadata currMetadata = null;
        try {
             currMetadata = schemaManager.getSchemaMetadata(ctx);
        } catch (VException eGet) {
            // If the client app did not set a schema as part of Database.Create(),
            // getSchemaMetadata() will return Errors.NO_EXIST. If so we set the schema
            // here.
            if (!eGet.getID().equals(io.v.v23.verror.Errors.NO_EXIST)) {
                throw eGet;
            }
            try {
                schemaManager.setSchemaMetadata(ctx, this.schema.getMetadata());
            } catch (VException eSet) {
                if (!eSet.getID().equals(io.v.v23.verror.Errors.NO_EXIST)) {
                    throw eSet;
                }
            }
            return false;
        }
        if (currMetadata.getVersion() >= this.schema.getMetadata().getVersion()) {
            return false;
        }
        // Call the Upgrader provided by the app to upgrade the schema.
        //
        // TODO(jlodhia): disable sync before running Upgrader and reenable
        // once Upgrader is finished.
        //
        // TODO(jlodhia): prevent other processes (local/remote) from accessing
        // the database while upgrade is in progress.
        this.schema.getUpgrader().run(this,
                currMetadata.getVersion(), this.schema.getMetadata().getVersion());

        // Update the schema metadata in db to the latest version.
        schemaManager.setSchemaMetadata(ctx, this.schema.getMetadata());
        return true;
    }

    // Implements BatchDatabase.
    @Override
    public void commit(VContext ctx) throws VException {
        this.client.commit(ctx, getSchemaVersion());
    }
    @Override
    public void abort(VContext ctx) throws VException {
        this.client.abort(ctx, getSchemaVersion());
    }

    private int getSchemaVersion() {
        if (this.schema == null) {
            return -1;
        }
        return this.schema.getMetadata().getVersion();
    }
}