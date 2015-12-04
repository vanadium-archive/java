// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import io.v.impl.google.naming.NamingUtil;
import io.v.v23.InputChannel;
import io.v.v23.InputChannels;
import io.v.v23.context.VContext;
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
    public ListenableFuture<List<String>> listTables(VContext ctx) {
        // See comment in v.io/v23/services/syncbase/nosql/service.vdl for why
        // we can't implement listTables using Glob (via Util.listChildren).
        return client.listTables(ctx);
    }
    @Override
    public ListenableFuture<QueryResults> exec(VContext ctx, String query) {
        final ClientRecvStream<List<VdlAny>, Void> stream =
                client.exec(ctx, getSchemaVersion(), query);
        return Futures.transform(stream.recv(),
                new AsyncFunction<List<VdlAny>, QueryResults>() {
                    @Override
                    public ListenableFuture<QueryResults> apply(List<VdlAny> columnNames)
                            throws Exception {
                        return Futures.immediateFuture(
                                (QueryResults) new QueryResultsImpl(columnNames, stream));
                    }
                });
    }

    // Implements AccessController interface.
    @Override
    public ListenableFuture<Void> setPermissions(VContext ctx, Permissions perms, String version) {
        return client.setPermissions(ctx, perms, version);
    }

    @Override
    public ListenableFuture<Map<String, Permissions>> getPermissions(VContext ctx) {
        return Futures.transform(client.getPermissions(ctx),
                new Function<ObjectClient.GetPermissionsOut, Map<String, Permissions>>() {
                    @Override
                    public Map<String, Permissions> apply(ObjectClient.GetPermissionsOut perms) {
                        return ImmutableMap.of(perms.version, perms.perms);
                    }
                });
    }

    // Implements Database interface.
    @Override
    public ListenableFuture<Boolean> exists(VContext ctx) {
        return client.exists(ctx, getSchemaVersion());
    }
    @Override
    public ListenableFuture<Void> create(VContext ctx, Permissions perms) {
        VdlOptional metadataOpt = schema != null
                ? VdlOptional.of(schema.getMetadata())
                : new VdlOptional<SchemaMetadata>(Types.optionalOf(SchemaMetadata.VDL_TYPE));
        return client.create(ctx, metadataOpt, perms);
    }
    @Override
    public ListenableFuture<Void> destroy(VContext ctx) {
        return client.destroy(ctx, getSchemaVersion());
    }
    public ListenableFuture<BatchDatabase> beginBatch(VContext ctx, BatchOptions opts) {
        return Futures.transform(client.beginBatch(ctx, getSchemaVersion(), opts),
                new Function<String, BatchDatabase>() {
                    @Override
                    public BatchDatabase apply(String batchSuffix) {
                        return new DatabaseImpl(parentFullName, name, batchSuffix, schema);
                    }
                });
    }
    @Override
    public InputChannel<WatchChange> watch(VContext ctx, String tableRelativeName,
                                           String rowPrefix, ResumeMarker resumeMarker) {
        return InputChannels.transform(client.watchGlob(ctx,
                new GlobRequest(NamingUtil.join(tableRelativeName, rowPrefix + "*"),
                        resumeMarker)),
                new InputChannels.TransformFunction<Change, WatchChange>() {
                    @Override
                    public WatchChange apply(Change change) throws VException {
                        return convertToWatchChange(change);
                    }
                });
    }
    @Override
    public ListenableFuture<ResumeMarker> getResumeMarker(VContext ctx) {
        return client.getResumeMarker(ctx);
    }
    @Override
    public Syncgroup getSyncgroup(String name) {
        return new SyncgroupImpl(fullName, name);
    }
    @Override
    public ListenableFuture<List<String>> listSyncgroupNames(VContext ctx) {
        return client.getSyncgroupNames(ctx);
    }
    @Override
    public ListenableFuture<BlobWriter> writeBlob(VContext ctx, BlobRef ref) {
        ListenableFuture<BlobRef> refFuture = ref == null
                                            ? client.createBlob(ctx)
                                            : Futures.immediateFuture(ref);
        return Futures.transform(refFuture, new Function<BlobRef, BlobWriter>() {
            @Override
            public BlobWriter apply(BlobRef ref) {
                return new BlobWriterImpl(client, ref);
            }
        });
    }
    @Override
    public BlobReader readBlob(VContext ctx, BlobRef ref) throws VException{
        if (ref == null) {
            throw new VException("Must pass a non-null blob ref.");
        }
        return new BlobReaderImpl(client, ref);
    }
    @Override
    public ListenableFuture<Boolean> upgradeIfOutdated(final VContext ctx) {
        if (schema == null) {
            Futures.immediateFailedFuture(new BadStateException(ctx));
        }
        if (schema.getMetadata().getVersion() < 0) {
            Futures.immediateFailedFuture(new BadStateException(ctx));
        }
        final SchemaManager schemaManager = new SchemaManager(fullName);
        final SettableFuture<Boolean> ret = SettableFuture.create();
        Futures.addCallback(schemaManager.getSchemaMetadata(ctx),
                new FutureCallback<SchemaMetadata>() {
            @Override
            public void onFailure(Throwable t) {
                if (t instanceof NoExistException) {
                    // If the client app did not set a schema as part of database creation,
                    // getSchemaMetadata() will throw NoExistException. In this case, we set the
                    // schema here.
                    Futures.addCallback(schemaManager.setSchemaMetadata(ctx, schema.getMetadata()),
                            new FutureCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    ret.set(false);
                                }
                                @Override
                                public void onFailure(Throwable t) {
                                    // The database may not yet exist. If so, setSchemaMetadata
                                    // will throw NoExistException, and here we return set the
                                    // return value of 'false'; otherwise, we fail the
                                    // return future.
                                    if (t instanceof NoExistException) {
                                        ret.set(false);
                                    } else {
                                        ret.setException(t);
                                    }
                                }
                            }
                    );
                } else {
                    ret.setException(t);
                }
            }
            @Override
            public void onSuccess(SchemaMetadata currMetadata) {
                // Call the Upgrader provided by the app to upgrade the schema.
                //
                // TODO(jlodhia): disable sync before running Upgrader and reenable
                // once Upgrader is finished.
                //
                // TODO(jlodhia): prevent other processes (local/remote) from accessing
                // the database while upgrade is in progress.
                try {
                    schema.getUpgrader().run(DatabaseImpl.this,
                            currMetadata.getVersion(), schema.getMetadata().getVersion());
                    // Update the schema metadata in db to the latest version.
                    Futures.addCallback(schemaManager.setSchemaMetadata(ctx, schema.getMetadata()),
                            new FutureCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    ret.set(true);
                                }
                                @Override
                                public void onFailure(Throwable t) {
                                    ret.setException(t);
                                }
                            });
                } catch (VException e) {
                    ret.setException(e);
                }
            }
        });
        return ret;
    }

    // Implements BatchDatabase.
    @Override
    public ListenableFuture<Void> commit(VContext ctx) {
        return client.commit(ctx, getSchemaVersion());
    }
    @Override
    public ListenableFuture<Void> abort(VContext ctx) {
        return client.abort(ctx, getSchemaVersion());
    }

    private int getSchemaVersion() {
        if (schema == null) {
            return -1;
        }
        return schema.getMetadata().getVersion();
    }

    private static class QueryResultsImpl implements QueryResults {
        private final InputChannel<List<VdlAny>> input;
        private final List<String> columnNames;

        private QueryResultsImpl(List<VdlAny> columnNames,
                                 InputChannel<List<VdlAny>> input) throws VException {
            this.input = input;
            this.columnNames = new ArrayList<>(columnNames.size());
            for (int i = 0; i < columnNames.size(); ++i) {
                Serializable elem = columnNames.get(i).getElem();
                if (elem instanceof String) {
                    this.columnNames.add((String) elem);
                } else {
                    throw new VException("Expected first row in exec() stream to contain column " +
                            "names (of type String), got type: " + elem.getClass());
                }
            }
        }
        @Override
        public ListenableFuture<List<VdlAny>> recv() {
            return input.recv();
        }
        @Override
        public List<String> columnNames() {
            return columnNames;
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
