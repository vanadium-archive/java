// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
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
import io.v.v23.security.access.Permissions;
import io.v.v23.services.permissions.ObjectClient;
import io.v.v23.services.syncbase.BatchOptions;
import io.v.v23.services.syncbase.BatchHandle;
import io.v.v23.services.syncbase.BlobRef;
import io.v.v23.services.syncbase.Id;
import io.v.v23.services.syncbase.DatabaseClient;
import io.v.v23.services.syncbase.DatabaseClientFactory;
import io.v.v23.services.syncbase.SchemaMetadata;
import io.v.v23.services.syncbase.StoreChange;
import io.v.v23.services.watch.Change;
import io.v.v23.services.watch.GlobRequest;
import io.v.v23.services.watch.ResumeMarker;
import io.v.v23.syncbase.util.Util;
import io.v.v23.vdl.ClientRecvStream;
import io.v.v23.vdl.Types;
import io.v.v23.vdl.VdlAny;
import io.v.v23.vdl.VdlOptional;
import io.v.v23.verror.NotImplementedException;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class DatabaseImpl implements Database, BatchDatabase {

    private final String parentFullName;
    private final String fullName;
    private final Id id;
    private final BatchHandle batchHandle;
    private final Schema schema;

    private final DatabaseClient client;

    DatabaseImpl(String parentFullName, Id id, Schema schema, BatchHandle batchHandle) {
        this.parentFullName = parentFullName;
        this.fullName = NamingUtil.join(parentFullName, Util.encodeId(id));
        this.id = id;
        this.batchHandle = batchHandle;
        this.schema = schema;
        this.client = DatabaseClientFactory.getDatabaseClient(this.fullName);
    }

    private static List<String> splitInTwo(String str, String separator) {
        Iterator<String> iter = Splitter.on(separator).limit(2).split(str).iterator();
        return ImmutableList.of(
                iter.hasNext() ? iter.next() : "", iter.hasNext() ? iter.next() : "");
    }

    // Implements DatabaseCore interface.
    @Override
    public Id id() {
        return this.id;
    }

    @Override
    public String fullName() {
        return this.fullName;
    }

    @Override
    public Collection getCollection(VContext ctx, String name) {
        String blessing = Util.UserBlessingFromContext(ctx);
        Id id = new Id(blessing, name);
        return getCollection(id);
    }

    public Collection getCollection(Id collectionId) {
        return new CollectionImpl(this.fullName, collectionId, this.batchHandle);
    }

    @Override
    public ListenableFuture<List<Id>> listCollections(VContext ctx) {
        // See comment in v.io/v23/services/syncbase/nosql/service.vdl for why
        // we can't implement listCollections using Glob (via Util.listChildren).
        return client.listCollections(ctx, this.batchHandle);
    }

    @Override
    public ListenableFuture<QueryResults> exec(VContext ctx, String query) {
        return this.execInternal(ctx, query, null);
    }

    @Override
    public ListenableFuture<QueryResults> exec(VContext ctx, String query,
                                               List<Object> paramValues, List<Type> paramTypes) {
        Preconditions.checkNotNull(paramValues);
        Preconditions.checkNotNull(paramTypes);
        if (paramValues.size() != paramTypes.size()) {
            throw new IllegalArgumentException("Length of paramValues and paramTypes is not equal");
        }
        List<VdlAny> params = new ArrayList<VdlAny>();
        try {
            Iterator<Object> v = paramValues.iterator();
            Iterator<Type> t = paramTypes.iterator();
            while (v.hasNext() && t.hasNext()) {
                params.add(new VdlAny(VomUtil.valueOf(v.next(), t.next())));
            }
        } catch (VException e) {
            return VFutures.withUserLandChecks(ctx, Futures.<QueryResults>immediateFailedFuture(e));
        }
        return this.execInternal(ctx, query, params);
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
        return client.exists(ctx);
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
        return client.destroy(ctx);
    }

    public ListenableFuture<BatchDatabase> beginBatch(VContext ctx, BatchOptions opts) {
        ListenableFuture<BatchHandle> batchFuture = client.beginBatch(ctx, opts);
        final String parentFullName = this.parentFullName;
        final Id id = this.id;
        final Schema schema = this.schema;
        return VFutures.withUserLandChecks(ctx,
                Futures.transform(batchFuture, new Function<BatchHandle, BatchDatabase>() {
                    @Override
                    public BatchDatabase apply(BatchHandle batchHandle) {
                        return new DatabaseImpl(parentFullName, id, schema, batchHandle);
                    }
                }));
    }

    @Override
    public InputChannel<WatchChange> watch(VContext ctx, Id collectionId,
                                           String prefix, ResumeMarker resumeMarker) {
        return InputChannels.transform(ctx, client.watchGlob(ctx,
                new GlobRequest(NamingUtil.join(Util.encodeId(collectionId), prefix + "*"),
                        resumeMarker)),
                new InputChannels.TransformFunction<Change, WatchChange>() {
                    @Override
                    public WatchChange apply(Change change) throws VException {
                        return convertToWatchChange(change);
                    }
                });
    }

    @Override
    public InputChannel<WatchChange> watch(VContext ctx, Id collectionId,
                                           String prefix) {
        return this.watch(ctx, collectionId, prefix, null);
    }

    @Override
    public ListenableFuture<ResumeMarker> getResumeMarker(VContext ctx) {
        return client.getResumeMarker(ctx, this.batchHandle);
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
    public BlobReader readBlob(VContext ctx, BlobRef ref) throws VException {
        if (ref == null) {
            throw new VException("Must pass a non-null blob ref.");
        }
        return new BlobReaderImpl(client, ref);
    }

    @Override
    public ListenableFuture<Void> enforceSchema(final VContext ctx) {
        ListenableFutureCallback<Void> callback = new ListenableFutureCallback<>();
        VException error = new NotImplementedException(ctx);
        callback.onFailure(error);
        return callback.getFuture(ctx);
    }

    // Implements BatchDatabase.
    @Override
    public ListenableFuture<Void> commit(VContext ctx) {
        return client.commit(ctx, this.batchHandle);
    }

    @Override
    public ListenableFuture<Void> abort(VContext ctx) {
        return client.abort(ctx, this.batchHandle);
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
        String[] parts = watchChange.getName().split("/", 2);
        if (parts.length != 2) {
            throw new VException("Invalid collection-row pair: " + watchChange.getName());
        }
        Id collectionId = Util.decodeId(parts[0]);
        String rowName = parts[1];
        return new WatchChange(collectionId, rowName, changeType, storeChange.getValue(),
                watchChange.getResumeMarker(), storeChange.getFromSync(),
                watchChange.getContinued());
    }

    private ListenableFuture<QueryResults> execInternal(VContext ctx, String query, List<VdlAny> params) {
        final ClientRecvStream<List<VdlAny>, Void> stream =
                client.exec(ctx, this.batchHandle, query, params);
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
}
