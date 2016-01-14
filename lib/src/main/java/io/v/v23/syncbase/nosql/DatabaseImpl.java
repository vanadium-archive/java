// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import io.v.impl.google.ListenableFutureCallback;
import io.v.impl.google.naming.NamingUtil;
import io.v.v23.InputChannel;
import io.v.v23.InputChannels;
import io.v.v23.VFutures;
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
import io.v.v23.vdl.ClientRecvStream;
import io.v.v23.vdl.Types;
import io.v.v23.vdl.VdlAny;
import io.v.v23.vdl.VdlOptional;
import io.v.v23.verror.VException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class DatabaseImpl implements Database, BatchDatabase {
    private static native DatabaseImpl nativeCreate(String parentFullName, String relativeName,
                                                    Schema schema) throws VException;

    static DatabaseImpl create(String parentFullName, String relativeName, Schema schema) {
        try {
            return nativeCreate(parentFullName, relativeName, schema);
        } catch (VException e) {
            throw new RuntimeException("Couldn't create database.", e);
        }
    }

    private native void nativeEnforceSchema(long nativePtr, VContext ctx, Callback<Void> callback);
    private native void nativeBeginBatch(long nativePtr, VContext ctx, BatchOptions opts,
                                         Callback<BatchDatabase> callback);
    private native void nativeFinalize(long nativePtr);

    private final long nativePtr;  // can be 0 (e.g., for BatchDatabase)
    private final String parentFullName;
    private final String fullName;
    private final String name;
    private final Schema schema;

    private final DatabaseClient client;

    private DatabaseImpl(long nativePtr, String parentFullName, String fullName,
                         String relativeName, Schema schema) {
        this.nativePtr = nativePtr;
        this.parentFullName = parentFullName;
        this.fullName = fullName;
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
        return VFutures.withUserLandChecks(ctx, Futures.transform(stream.recv(),
                new AsyncFunction<List<VdlAny>, QueryResults>() {
                    @Override
                    public ListenableFuture<QueryResults> apply(List<VdlAny> columnNames)
                            throws Exception {
                        return Futures.immediateFuture(
                                (QueryResults) new QueryResultsImpl(columnNames, stream));
                    }
                }));
    }

    // Implements AccessController interface.
    @Override
    public ListenableFuture<Void> setPermissions(VContext ctx, Permissions perms, String version) {
        return client.setPermissions(ctx, perms, version);
    }

    @Override
    public ListenableFuture<Map<String, Permissions>> getPermissions(VContext ctx) {
        return VFutures.withUserLandChecks(ctx, Futures.transform(client.getPermissions(ctx),
                new Function<ObjectClient.GetPermissionsOut, Map<String, Permissions>>() {
                    @Override
                    public Map<String, Permissions> apply(ObjectClient.GetPermissionsOut perms) {
                        return ImmutableMap.of(perms.version, perms.perms);
                    }
                }));
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
        ListenableFutureCallback<BatchDatabase> callback = new ListenableFutureCallback<>();
        if (nativePtr == 0) {
            throw new RuntimeException("beginBatch() called with zero nativePtr - is it called " +
                    "from within BatchDatabase?");
        }
        nativeBeginBatch(nativePtr, ctx, opts, callback);
        return callback.getFuture(ctx);
    }
    @Override
    public InputChannel<WatchChange> watch(VContext ctx, String tableRelativeName,
                                           String rowPrefix, ResumeMarker resumeMarker) {
        return InputChannels.transform(ctx, client.watchGlob(ctx,
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
        return VFutures.withUserLandChecks(ctx,
                Futures.transform(refFuture, new Function<BlobRef, BlobWriter>() {
            @Override
            public BlobWriter apply(BlobRef ref) {
                return new BlobWriterImpl(client, ref);
            }
        }));
    }
    @Override
    public BlobReader readBlob(VContext ctx, BlobRef ref) throws VException{
        if (ref == null) {
            throw new VException("Must pass a non-null blob ref.");
        }
        return new BlobReaderImpl(client, ref);
    }
    @Override
    public ListenableFuture<Void> enforceSchema(final VContext ctx) {
        ListenableFutureCallback<Void> callback = new ListenableFutureCallback<>();
        if (nativePtr == 0) {
            throw new RuntimeException("enforceSchema() called with zero nativePtr - is it " +
                    "called from within BatchDatabase?");
        }
        nativeEnforceSchema(nativePtr, ctx, callback);
        return callback.getFuture(ctx);
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
    @Override
    protected void finalize() {
        if (nativePtr != 0) {
            nativeFinalize(nativePtr);
        }
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
