// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.v.impl.google.naming.NamingUtil;
import io.v.v23.VIterable;
import io.v.v23.VIterables;
import io.v.v23.context.CancelableVContext;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Callback;
import io.v.v23.security.access.Permissions;
import io.v.v23.services.permissions.ObjectClient;
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
import io.v.v23.vdl.ClientRecvStream;
import io.v.v23.vdl.Types;
import io.v.v23.vdl.VdlAny;
import io.v.v23.vdl.VdlOptional;
import io.v.v23.verror.BadStateException;
import io.v.v23.verror.NoExistException;
import io.v.v23.verror.VException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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
        return new QueryResultsImpl(client.exec(ctx, getSchemaVersion(), query));
    }

    // Implements AccessController interface.
    @Override
    public void setPermissions(VContext ctx, Permissions perms, String version) throws VException {
        this.client.setPermissions(ctx, perms, version);
    }

    @Override
    public void setPermissions(VContext ctx, Permissions perms, String version,
                               Callback<Void> callback) throws VException {
        client.setPermissions(ctx, perms, version, callback);
    }

    @Override
    public Map<String, Permissions> getPermissions(VContext ctx) throws VException {
        DatabaseClient.GetPermissionsOut perms = this.client.getPermissions(ctx);
        return ImmutableMap.of(perms.version, perms.perms);
    }

    @Override
    public void getPermissions(VContext ctx, final Callback<Map<String, Permissions>> callback)
            throws VException {
        client.getPermissions(ctx, new Callback<ObjectClient.GetPermissionsOut>() {
            @Override
            public void onSuccess(ObjectClient.GetPermissionsOut result) {
                callback.onSuccess(ImmutableMap.of(result.version, result.perms));
            }

            @Override
            public void onFailure(VException error) {
                callback.onFailure(error);
            }
        });
    }

    // Implements Database interface.
    @Override
    public boolean exists(VContext ctx) throws VException {
        return this.client.exists(ctx, getSchemaVersion());
    }
    @Override
    public void exists(VContext ctx, Callback<Boolean> callback) throws VException {
        client.exists(ctx, getSchemaVersion(), callback);
    }
    @Override
    public void create(VContext ctx, Permissions perms) throws VException {
        VdlOptional metadataOpt = this.schema != null
                ? VdlOptional.of(this.schema.getMetadata())
                : new VdlOptional<SchemaMetadata>(Types.optionalOf(SchemaMetadata.VDL_TYPE));
        this.client.create(ctx, metadataOpt, perms);
    }
    @Override
    public void create(VContext ctx, Permissions perms, Callback<Void> callback) throws VException {
        VdlOptional metadataOpt = this.schema != null
                ? VdlOptional.of(this.schema.getMetadata())
                : new VdlOptional<SchemaMetadata>(Types.optionalOf(SchemaMetadata.VDL_TYPE));
        client.create(ctx, metadataOpt, perms, callback);
    }
    @Override
    public void destroy(VContext ctx) throws VException {
        this.client.destroy(ctx, getSchemaVersion());
    }
    @Override
    public void destroy(VContext ctx, Callback<Void> callback) throws VException {
        client.destroy(ctx, getSchemaVersion(), callback);
    }
    public BatchDatabase beginBatch(VContext ctx, BatchOptions opts) throws VException {
        String batchSuffix = this.client.beginBatch(ctx, getSchemaVersion(), opts);
        return new DatabaseImpl(this.parentFullName, this.name, batchSuffix, this.schema);
    }
    public void beginBatch(VContext ctx, BatchOptions opts, final Callback<BatchDatabase> callback) throws VException {
        client.beginBatch(ctx, getSchemaVersion(), opts, new Callback<String>() {
            @Override
            public void onSuccess(String result) {
                callback.onSuccess(new DatabaseImpl(parentFullName, name, result, schema));
            }

            @Override
            public void onFailure(VException error) {
                callback.onFailure(error);
            }
        });
    }
    @Override
    public VIterable<WatchChange> watch(VContext ctx, String tableRelativeName, String rowPrefix,
                                        ResumeMarker resumeMarker) throws VException {
        return VIterables.transform(
                client.watchGlob(ctx,
                        new GlobRequest(NamingUtil.join(tableRelativeName, rowPrefix + "*"),
                                resumeMarker)),
                new VIterables.TransformFunction<Change, WatchChange>() {
                    @Override
                    public WatchChange apply(Change input) throws VException {
                        return convertToWatchChange(input);
                    }
                });
    }

    @Override
    public void watch(VContext ctx, String tableRelativeName, String rowPrefix,
                      ResumeMarker resumeMarker, final Callback<VIterable<WatchChange>> callback)
            throws VException {
        final CancelableVContext ctxC = ctx.withCancel();
        Callback<ClientRecvStream<Change, Void>> watchCallback =
                new Callback<ClientRecvStream<Change, Void>>() {
            @Override
            public void onSuccess(ClientRecvStream<Change, Void> result) {
                callback.onSuccess(
                        VIterables.transform(result,
                                new VIterables.TransformFunction<Change, WatchChange>() {
                                    @Override
                                    public WatchChange apply(Change input) throws VException {
                                        return convertToWatchChange(input);
                                    }
                                }));
            }
            @Override
            public void onFailure(VException error) {
                callback.onFailure(error);
            }
        };
        client.watchGlob(ctxC, new GlobRequest(NamingUtil.join(tableRelativeName, rowPrefix +
                "*"), resumeMarker), watchCallback);
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
    public List<String> listSyncgroupNames(VContext ctx) throws VException {
        return client.getSyncgroupNames(ctx);
    }
    @Override
    public void listSyncgroupNames(VContext ctx, Callback<List<String>> callback)
            throws VException {
        client.getSyncgroupNames(ctx, callback);
    }
    @Override
    public BlobWriter writeBlob(VContext ctx, BlobRef ref) throws VException {
        if (ref == null) {
            ref = client.createBlob(ctx);
        }
        return new BlobWriterImpl(client, ref);
    }
    @Override
    public void writeBlob(VContext ctx, BlobRef ref, final Callback<BlobWriter> callback)
            throws VException {
        if (ref != null) {
            callback.onSuccess(new BlobWriterImpl(client, ref));
        } else {
            client.createBlob(ctx, new Callback<BlobRef>() {
                @Override
                public void onSuccess(BlobRef result) {
                    callback.onSuccess(new BlobWriterImpl(client, result));
                }

                @Override
                public void onFailure(VException error) {
                    callback.onFailure(error);
                }
            });
        }
    }
    @Override
    public BlobReader readBlob(VContext ctx, BlobRef ref) throws VException {
        if (ref == null) {
            throw new VException("Must pass a non-null blob ref.");
        }
        return new BlobReaderImpl(client, ref);
    }
    @Override
    public void readBlob(VContext ctx, BlobRef ref, Callback<BlobReader> callback)
            throws VException {
        callback.onSuccess(readBlob(ctx, ref));
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

    @Override
    public void upgradeIfOutdated(final VContext ctx, final Callback<Boolean> callback)
            throws VException {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.onSuccess(upgradeIfOutdated(ctx));
                } catch (VException error) {
                    callback.onFailure(error);
                }
            }
        });
        thread.setName("DB update thread");
        thread.start();
    }

    // Implements BatchDatabase.
    @Override
    public void commit(VContext ctx) throws VException {
        this.client.commit(ctx, getSchemaVersion());
    }
    @Override
    public void commit(VContext ctx, Callback<Void> callback) throws VException {
        client.commit(ctx, getSchemaVersion(), callback);
    }
    @Override
    public void abort(VContext ctx) throws VException {
        this.client.abort(ctx, getSchemaVersion());
    }
    @Override
    public void abort(VContext ctx, Callback<Void> callback) throws VException {
        client.abort(ctx, getSchemaVersion(), callback);
    }

    private int getSchemaVersion() {
        if (this.schema == null) {
            return -1;
        }
        return this.schema.getMetadata().getVersion();
    }

    private static class QueryResultsImpl implements QueryResults {
        private final VIterable<List<VdlAny>> iterable;
        private final Iterator<List<VdlAny>> it;
        private boolean calledIterator;
        private final List<String> columnNames;

        private QueryResultsImpl(VIterable<List<VdlAny>> iterable) throws VException {
            this.iterable = iterable;
            // Iterator can be created only once, so cache the iterator here as we need
            // to use it just below.
            it = iterable.iterator();

            // The first row should contain column names, pull them off the stream.
            List<VdlAny> row;
            try {
                row = it.next();
            } catch (NoSuchElementException e) {
                throw new VException("Got empty exec() stream.");
            }
            columnNames = new ArrayList(row.size());
            for (int i = 0; i < row.size(); ++i) {
                Serializable elem = row.get(i).getElem();
                if (elem instanceof String) {
                    columnNames.add((String) elem);
                } else {
                    throw new VException("Expected first row in exec() stream to contain column " +
                            "names (of type String), got type: " + elem.getClass());
                }
            }
        }
        @Override
        public Iterator<List<VdlAny>> iterator() {
            Preconditions.checkState(!calledIterator, "Can only create one iterator.");
            calledIterator = true;
            return it;
        }
        @Override
        public VException error() {
            return iterable.error();
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
