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

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.v.impl.google.naming.NamingUtil;
import io.v.v23.services.syncbase.nosql.BatchOptions;
import io.v.v23.services.syncbase.nosql.DatabaseClient;
import io.v.v23.services.syncbase.nosql.DatabaseClientFactory;
import io.v.v23.services.syncbase.nosql.SchemaMetadata;
import io.v.v23.services.syncbase.nosql.StoreChange;
import io.v.v23.services.syncbase.nosql.TableClient;
import io.v.v23.services.syncbase.nosql.TableClientFactory;
import io.v.v23.services.watch.Change;
import io.v.v23.services.watch.GlobRequest;
import io.v.v23.services.watch.ResumeMarker;
import io.v.v23.syncbase.Syncbase;
import io.v.v23.syncbase.util.Util;
import io.v.v23.context.CancelableVContext;
import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;
import io.v.v23.vdl.TypedClientStream;
import io.v.v23.vdl.Types;
import io.v.v23.vdl.VdlAny;
import io.v.v23.vdl.VdlOptional;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

class DatabaseImpl implements Database, BatchDatabase {
    private final String parentFullName;
    private final String fullName;
    private final String name;
    private final Schema schema;
    private final DatabaseClient client;

    DatabaseImpl(String parentFullName, String relativeName, Schema schema) {
        this.parentFullName = parentFullName;
        this.fullName = NamingUtil.join(parentFullName, Util.NAME_SEP, relativeName);
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
        List<String> x = this.client.listTables(ctx);
        return x.toArray(new String[x.size()]);
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
    public void destroy(VContext ctx) throws VException {
        this.client.destroy(ctx, getSchemaVersion());
    }
    public BatchDatabase beginBatch(VContext ctx, BatchOptions opts) throws VException {
        String relativeName = this.client.beginBatch(ctx, getSchemaVersion(), opts);
        return new DatabaseImpl(this.parentFullName, relativeName, this.schema);
    }
    @Override
    public Stream<WatchChange> watch(VContext ctx, String tableRelativeName, String rowPrefix,
                                     ResumeMarker resumeMarker) throws VException {
        CancelableVContext ctxC = ctx.withCancel();
        TypedClientStream<Void, Change, Void> stream = this.client.watchGlob(ctxC,
                new GlobRequest(NamingUtil.join(tableRelativeName, Util.NAME_SEP, rowPrefix + "*"), resumeMarker));
        return new WatchChangeStreamImpl(ctxC, stream);
    }
    @Override
    public ResumeMarker getResumeMarker(VContext ctx) throws VException {
        return this.client.getResumeMarker(ctx);
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

    private static class ResultStreamImpl implements ResultStream {
        private final CancelableVContext ctxC;
        private final TypedClientStream<Void, List<VdlAny>, Void> stream;
        private final List<String> columnNames;
        private volatile boolean isCanceled;
        private volatile boolean isCreated;

        private ResultStreamImpl(CancelableVContext ctxC, TypedClientStream<Void,
                List<VdlAny>, Void> stream, List<String> columnNames) {
            this.ctxC = ctxC;
            this.stream = stream;
            this.columnNames = columnNames;
            this.isCanceled = this.isCreated = false;
        }
        // Implements Stream<List<VdlAny>>.
        @Override
        public synchronized Iterator<List<VdlAny>> iterator() {
            if (isCreated) {
                throw new RuntimeException("Can only create one ResultStream iterator.");
            }
            isCreated = true;
            return new AbstractIterator<List<VdlAny>>() {
                @Override
                protected List<VdlAny> computeNext() {
                    synchronized (ResultStreamImpl.this) {
                        if (isCanceled) {  // client canceled the stream
                            return endOfData();
                        }
                        try {
                            return stream.recv();
                        } catch (EOFException e) {  // legitimate end of stream
                            return endOfData();
                        } catch (VException e) {
                            if (isCanceled) {
                                return endOfData();
                            }
                            throw new RuntimeException("Error retrieving next stream element.", e);
                        }
                    }
                }
            };
        }
        @Override
        public synchronized void cancel() throws VException {
            this.isCanceled = true;
            this.ctxC.cancel();
        }
        // Implements ResultStream.
        @Override
        public List<String> columnNames() {
            return this.columnNames;
        }
    }

    private static class WatchChangeStreamImpl implements Stream<WatchChange> {
        private final CancelableVContext ctxC;
        private final TypedClientStream<Void, Change, Void> stream;
        private volatile boolean isCanceled;
        private volatile boolean isCreated;


        private WatchChangeStreamImpl(CancelableVContext ctxC,
                                      TypedClientStream<Void, Change, Void> stream) {
            this.ctxC = ctxC;
            this.stream = stream;
            this.isCanceled = this.isCreated = false;
        }
        // Implements Stream<WatchChange>.
        @Override
        public synchronized Iterator<WatchChange> iterator() {
            if (isCreated) {
                throw new RuntimeException("Can only create one ResultStream iterator.");
            }
            isCreated = true;
            return new AbstractIterator<WatchChange>() {
                @Override
                protected WatchChange computeNext() {
                    synchronized (WatchChangeStreamImpl.this) {
                        if (isCanceled) {  // client canceled the stream
                            return endOfData();
                        }
                        try {
                            return convert(stream.recv());
                        } catch (EOFException e) {  // legitimate end of stream
                            return endOfData();
                        } catch (VException e) {
                            if (isCanceled) {
                                return endOfData();
                            }
                            throw new RuntimeException("Error retrieving next stream element.", e);
                        }
                    }
                }
            };
        }
        @Override
        public synchronized void cancel() throws VException {
            isCanceled = true;
            ctxC.cancel();
        }

        private WatchChange convert(Change watchChange) throws VException {
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
            List<String> parts = splitInTwo(watchChange.getName(), Util.NAME_SEP_WITH_SLASHES);
            String tableName = parts.get(0);
            if (!Util.isValidName(tableName)) {
                throw new VException("Invalid table name: \"" + tableName + "\"" + " in change: " +
                        watchChange);
            }
            String rowName = parts.get(1);
            if (!Util.isValidName(rowName)) {
                throw new VException("Invalid row name: \"" + rowName + "\"" + " in change: " +
                        watchChange);
            }
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
}