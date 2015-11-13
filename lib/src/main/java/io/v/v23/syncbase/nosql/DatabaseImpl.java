// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import java.io.EOFException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import com.google.common.collect.Iterators;
import io.v.impl.google.naming.NamingUtil;
import io.v.v23.VIterable;
import io.v.v23.services.syncbase.nosql.BatchOptions;
import io.v.v23.services.syncbase.nosql.BlobRef;
import io.v.v23.services.syncbase.nosql.DatabaseClient;
import io.v.v23.services.syncbase.nosql.DatabaseClientFactory;
import io.v.v23.services.syncbase.nosql.SchemaMetadata;
import io.v.v23.services.syncbase.nosql.StoreChange;
import io.v.v23.services.watch.Change;
import io.v.v23.services.watch.GlobRequest;
import io.v.v23.services.watch.ResumeMarker;
import io.v.v23.syncbase.util.Util;
import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;
import io.v.v23.vdl.TypedClientStream;
import io.v.v23.vdl.TypedStreamIterable;
import io.v.v23.vdl.Types;
import io.v.v23.vdl.VdlAny;
import io.v.v23.vdl.VdlOptional;
import io.v.v23.verror.BadStateException;
import io.v.v23.verror.NoExistException;
import io.v.v23.verror.VException;

class DatabaseImpl implements Database, BatchDatabase {
    private final String parentFullName;
    private final String fullName;
    private final String name;
    private final Schema schema;
    private final DatabaseClient client;

    DatabaseImpl(String parentFullName, String relativeName, String batchSuffix, Schema schema) {
        this.parentFullName = parentFullName;
        // Escape relativeName so that any forward slashes get dropped, thus
        // ensuring that the server will interpret fullName as referring to a
        // database object. Note that the server will still reject this name if
        // util.ValidDatabaseName returns false.
        this.fullName = NamingUtil.join(parentFullName, Util.escape(relativeName) + batchSuffix);
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
        // See comment in v.io/v23/services/syncbase/nosql/service.vdl for why
        // we can't implement listTables using Glob (via Util.listChildren).
        List<String> x = this.client.listTables(ctx);
        return x.toArray(new String[x.size()]);
    }
    @Override
    public QueryResults exec(VContext ctx, String query) throws VException {
        TypedClientStream<Void, List<VdlAny>, Void> stream =
                this.client.exec(ctx, getSchemaVersion(), query);

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
        return new QueryResultsImpl(stream, Arrays.asList(columnNames));
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
    public void destroy(VContext ctx) throws VException {
        this.client.destroy(ctx, getSchemaVersion());
    }
    public BatchDatabase beginBatch(VContext ctx, BatchOptions opts) throws VException {
        String batchSuffix = this.client.beginBatch(ctx, getSchemaVersion(), opts);
        return new DatabaseImpl(this.parentFullName, this.name, batchSuffix, this.schema);
    }
    @Override
    public VIterable<WatchChange> watch(VContext ctx, String tableRelativeName, String rowPrefix,
                                        ResumeMarker resumeMarker) throws VException {
        TypedClientStream<Void, Change, Void> stream = this.client.watchGlob(ctx,
                new GlobRequest(NamingUtil.join(tableRelativeName, rowPrefix + "*"), resumeMarker));
        return new TypedStreamIterable(stream) {
            @Override
            public synchronized Iterator iterator() {
                return Iterators.transform(super.iterator(), new Function<Change, WatchChange>() {
                    @Override
                    public WatchChange apply(Change change) {
                        try {
                            return convertToWatchChange(change);
                        } catch (VException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
            }
        };
    }
    @Override
    public ResumeMarker getResumeMarker(VContext ctx) throws VException {
        return this.client.getResumeMarker(ctx);
    }
    @Override
    public Syncgroup getSyncgroup(String name) {
        return new SyncgroupImpl(this.fullName, name);
    }
    @Override
    public String[] listSyncgroupNames(VContext ctx) throws VException {
        List<String> names = this.client.getSyncgroupNames(ctx);
        return names.toArray(new String[names.size()]);
    }
    @Override
    public BlobWriter writeBlob(VContext ctx, BlobRef ref) throws VException {
        if (ref == null) {
            ref = client.createBlob(ctx);
        }
        return new BlobWriterImpl(client, ref);
    }
    @Override
    public BlobReader readBlob(VContext ctx, BlobRef ref) throws VException {
        if (ref == null) {
            throw new VException("Must pass a non-null blob ref.");
        }
        return new BlobReaderImpl(client, ref);
    }

    @Override
    public boolean upgradeIfOutdated(VContext ctx) throws VException {
        if (this.schema == null) {
            throw new BadStateException(ctx);
        }
        if (this.schema.getMetadata().getVersion() < 0) {
            throw new BadStateException(ctx);
        }
        SchemaManager schemaManager = new SchemaManager(this.fullName);
        SchemaMetadata currMetadata = null;
        try {
            currMetadata = schemaManager.getSchemaMetadata(ctx);
        } catch (NoExistException eFirst) {
            // If the client app did not set a schema as part of Database.Create(),
            // getSchemaMetadata() will throws NoExistException. If so we set the schema
            // here.
            try {
                schemaManager.setSchemaMetadata(ctx, this.schema.getMetadata());
            } catch (NoExistException eSecond) {
                return false;
            }
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

    private static class QueryResultsImpl
            extends TypedStreamIterable<List<VdlAny>> implements QueryResults {
        private final List<String> columnNames;

        private QueryResultsImpl(TypedClientStream<Void, List<VdlAny>, Void> stream,
                            List<String> columnNames) {
            super(stream);
            this.columnNames = columnNames;
        }
        @Override
        public List<String> columnNames() {
            return this.columnNames;
        }
    }

    private WatchChange convertToWatchChange(Change watchChange) throws VException {
        Object value = watchChange.getValue().getElem();
        if (!(value instanceof StoreChange)) {
            throw new VException("Expected watch data to contain StoreChange, instead got: "
                    + value);
        }
        StoreChange storeChange = (StoreChange) value;
        ChangeType changeType;
        switch (watchChange.getState()) {
            case io.v.v23.services.watch.Constants.EXISTS:
                changeType = ChangeType.PUT_CHANGE;
                break;
            case io.v.v23.services.watch.Constants.DOES_NOT_EXIST:
                changeType = ChangeType.DELETE_CHANGE;
                break;
            default:
                throw new VException(
                        "Unsupported watch change state: " + watchChange.getState());
        }
        List<String> parts = splitInTwo(watchChange.getName(), "/");
        String tableName = parts.get(0);
        String rowName = parts.get(1);
        return new WatchChange(tableName, rowName, changeType, storeChange.getValue(),
                watchChange.getResumeMarker(), storeChange.getFromSync(),
                watchChange.getContinued());
    }

    private static List<String> splitInTwo(String str, String separator) {
        Iterator<String> iter = Splitter.on(separator).limit(2).split(str).iterator();
        return ImmutableList.of(
                iter.hasNext() ? iter.next() : "", iter.hasNext() ? iter.next() : "");
    }
}
