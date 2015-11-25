// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import io.v.impl.google.naming.NamingUtil;
import io.v.impl.google.services.syncbase.SyncbaseServer;
import io.v.v23.VIterable;
import io.v.v23.context.CancelableVContext;
import io.v.v23.naming.Endpoint;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.services.syncbase.nosql.BlobFetchStatus;
import io.v.v23.services.syncbase.nosql.BlobRef;
import io.v.v23.services.syncbase.nosql.KeyValue;
import io.v.v23.services.syncbase.nosql.SyncgroupMemberInfo;
import io.v.v23.services.syncbase.nosql.TableRow;
import io.v.v23.services.syncbase.nosql.SyncgroupSpec;
import io.v.v23.services.watch.ResumeMarker;
import io.v.v23.syncbase.nosql.BatchDatabase;
import io.v.v23.syncbase.nosql.BlobReader;
import io.v.v23.syncbase.nosql.BlobWriter;
import io.v.v23.syncbase.nosql.ChangeType;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.DatabaseCore;
import io.v.v23.syncbase.nosql.Row;
import io.v.v23.syncbase.nosql.RowRange;
import io.v.v23.syncbase.nosql.Syncgroup;
import io.v.v23.syncbase.nosql.Table;
import io.v.v23.syncbase.util.Util;
import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Server;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.access.AccessList;
import io.v.v23.security.access.Constants;
import io.v.v23.security.access.Permissions;
import io.v.v23.syncbase.nosql.WatchChange;
import io.v.v23.vdl.VdlAny;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;
import junit.framework.TestCase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static com.google.common.truth.Truth.assertThat;
import static io.v.v23.VFutures.sync;

/**
 * Client-server syncbase tests.
 */
public class SyncbaseTest extends TestCase {
    private static final String APP_NAME = "app/a#%b";  // symbols are okay
    private static final String DB_NAME = "db";
    private static final String TABLE_NAME = "table";
    private static final String ROW_NAME = "row/a#%b";  // symbols are okay

    private VContext ctx;
    private Permissions allowAll;
    private Endpoint serverEndpoint;

    @Override
    protected void setUp() throws Exception {
        ctx = V.init();
        ctx = V.withListenSpec(ctx, V.getListenSpec(ctx).withAddress(
                new ListenSpec.Address("tcp", "localhost:0")));
        AccessList acl = new AccessList(
                ImmutableList.of(new BlessingPattern("...")), ImmutableList.<String>of());
        allowAll = new Permissions(ImmutableMap.of(
                Constants.READ.getValue(), acl,
                Constants.WRITE.getValue(), acl,
                Constants.ADMIN.getValue(), acl));
        String tmpDir = Files.createTempDir().getAbsolutePath();
        ctx = SyncbaseServer.withNewServer(ctx, new SyncbaseServer.Params()
                .withPermissions(allowAll)
                .withStorageRootDir(tmpDir));
        Server server = V.getServer(ctx);
        assertThat(server).isNotNull();
        Endpoint[] endpoints = server.getStatus().getEndpoints();
        assertThat(endpoints).isNotEmpty();
        serverEndpoint = endpoints[0];
    }

    @Override
    protected void tearDown() throws Exception {
        Server server = V.getServer(ctx);
        if (server != null) {
            server.stop();
        }
        V.shutdown();
    }

    public void testService() throws Exception {
        SyncbaseService service = createService();
        assertThat(service.fullName()).isEqualTo(serverEndpoint.name());
        assertThat(sync(service.listApps(ctx))).isEmpty();
    }

    public void testApp() throws Exception {
        SyncbaseService service = createService();
        SyncbaseApp app = service.getApp(APP_NAME);
        assertThat(app).isNotNull();
        assertThat(app.name()).isEqualTo(APP_NAME);
        assertThat(app.fullName()).isEqualTo(
            NamingUtil.join(serverEndpoint.name(), Util.escape(APP_NAME)));
        assertThat(sync(app.exists(ctx))).isFalse();
        assertThat(sync(service.listApps(ctx))).isEmpty();
        sync(app.create(ctx, allowAll));
        assertThat(sync(app.exists(ctx))).isTrue();
        assertThat(sync(service.listApps(ctx))).containsExactly(app.name());
        assertThat(sync(app.listDatabases(ctx))).isEmpty();
        sync(app.destroy(ctx));
        assertThat(sync(app.exists(ctx))).isFalse();
        assertThat(sync(service.listApps(ctx))).isEmpty();
    }

    public void testDatabase() throws Exception {
        SyncbaseApp app = createApp(createService());
        assertThat(app).isNotNull();
        Database db = app.getNoSqlDatabase("db", null);
        assertThat(db).isNotNull();
        assertThat(db.name()).isEqualTo(DB_NAME);
        assertThat(db.fullName()).isEqualTo(
                NamingUtil.join(serverEndpoint.name(), Util.escape(APP_NAME), DB_NAME));
        assertThat(sync(db.exists(ctx))).isFalse();
        assertThat(sync(app.listDatabases(ctx))).isEmpty();
        sync(db.create(ctx, allowAll));
        assertThat(sync(db.exists(ctx))).isTrue();
        assertThat(sync(app.listDatabases(ctx))).containsExactly(db.name());
        assertThat(sync(db.listTables(ctx))).isEmpty();
        sync(db.destroy(ctx));
        assertThat(sync(db.exists(ctx))).isFalse();
        assertThat(sync(app.listDatabases(ctx))).isEmpty();
    }

    public void testTable() throws Exception {
        Database db = createDatabase(createApp(createService()));
        assertThat(db).isNotNull();
        Table table = db.getTable(TABLE_NAME);
        assertThat(table).isNotNull();
        assertThat(table.name()).isEqualTo(TABLE_NAME);
        assertThat(table.fullName()).isEqualTo(
            NamingUtil.join(serverEndpoint.name(), Util.escape(APP_NAME), DB_NAME, TABLE_NAME));
        assertThat(sync(table.exists(ctx))).isFalse();
        assertThat(sync(db.listTables(ctx))).isEmpty();
        sync(table.create(ctx, allowAll));
        assertThat(sync(table.exists(ctx))).isTrue();
        assertThat(sync(db.listTables(ctx))).containsExactly(TABLE_NAME);

        assertThat(sync(table.getRow("row1").exists(ctx))).isFalse();
        sync(table.put(ctx, "row1", "value1", String.class));
        assertThat(sync(table.getRow("row1").exists(ctx))).isTrue();
        assertThat(sync(table.get(ctx, "row1", String.class))).isEqualTo("value1");
        sync(table.delete(ctx, "row1"));
        assertThat(sync(table.getRow("row1").exists(ctx))).isFalse();
        sync(table.put(ctx, "row1", "value1", String.class));
        sync(table.put(ctx, "row2", "value2", String.class));
        assertThat(sync(table.getRow("row1").exists(ctx))).isTrue();
        assertThat(sync(table.getRow("row2").exists(ctx))).isTrue();
        assertThat(sync(table.get(ctx, "row1", String.class))).isEqualTo("value1");
        assertThat(sync(table.get(ctx, "row2", String.class))).isEqualTo("value2");
        assertThat(sync(table.scan(ctx, RowRange.range("row1", "row3")))).containsExactly(
                new KeyValue("row1", VomUtil.encode("value1", String.class)),
                new KeyValue("row2", VomUtil.encode("value2", String.class)));
        sync(table.deleteRange(ctx, RowRange.range("row1", "row3")));
        assertThat(sync(table.getRow("row1").exists(ctx))).isFalse();
        assertThat(sync(table.getRow("row2").exists(ctx))).isFalse();

        sync(table.destroy(ctx));
        assertThat(sync(table.exists(ctx))).isFalse();
        assertThat(sync(db.listTables(ctx))).isEmpty();
    }

    public void testRow() throws Exception {
        Table table = createTable(createDatabase(createApp(createService())));
        Row row = table.getRow(ROW_NAME);
        assertThat(row).isNotNull();
        assertThat(row.key()).isEqualTo(ROW_NAME);
        assertThat(row.fullName()).isEqualTo(
                NamingUtil.join(serverEndpoint.name(), Util.escape(APP_NAME), DB_NAME, TABLE_NAME,
                        Util.escape(ROW_NAME)));
        assertThat(sync(row.exists(ctx))).isFalse();
        sync(row.put(ctx, "value", String.class));
        assertThat(sync(row.exists(ctx))).isTrue();
        assertThat(sync(row.get(ctx, String.class))).isEqualTo("value");
        assertThat(sync(table.get(ctx, ROW_NAME, String.class))).isEqualTo("value");
        sync(row.delete(ctx));
        assertThat(sync(row.exists(ctx))).isFalse();
        sync(table.put(ctx, ROW_NAME, "value", String.class));
        assertThat(sync(row.exists(ctx))).isTrue();
        assertThat(sync(row.get(ctx, String.class))).isEqualTo("value");
        assertThat(sync(table.get(ctx, ROW_NAME, String.class))).isEqualTo("value");
    }

    public void testDatabaseExec() throws Exception {
        Database db = createDatabase(createApp(createService()));
        Table table = createTable(db);
        Foo foo = new Foo(4, "f");
        Bar bar = new Bar(0.5f, "b");
        Baz baz = new Baz("John Doe", true);

        sync(table.put(ctx, "foo", foo, Foo.class));
        sync(table.put(ctx, "bar", bar, Bar.class));
        sync(table.put(ctx, "baz", baz, Baz.class));

        {
            DatabaseCore.QueryResults results = sync(db.exec(ctx,
                    "select k, v.Name from " + TABLE_NAME + " where Type(v) like \"%Baz\""));
            assertThat(results.columnNames()).containsExactly("k", "v.Name");
            assertThat(results).containsExactly(ImmutableList.of(
                    new VdlAny(String.class, "baz"), new VdlAny(String.class, baz.name)));
        }
        {
            DatabaseCore.QueryResults results =
                    sync(db.exec(ctx, "select k, v from " + TABLE_NAME));
            assertThat(results.columnNames()).containsExactly("k", "v");
            assertThat(results).containsExactly(
                    ImmutableList.of(new VdlAny(String.class, "bar"), new VdlAny(Bar.class, bar)),
                    ImmutableList.of(new VdlAny(String.class, "baz"), new VdlAny(Baz.class, baz)),
                    ImmutableList.of(new VdlAny(String.class, "foo"), new VdlAny(Foo.class, foo))
            );
        }
    }

    public void testDatabaseWatch() throws Exception {
        Database db = createDatabase(createApp(createService()));
        Table table = createTable(db);
        Foo foo = new Foo(4, "f");
        Bar bar = new Bar(0.5f, "b");
        Baz baz = new Baz("John Doe", true);
        ResumeMarker marker = sync(db.getResumeMarker(ctx));

        sync(table.put(ctx, "foo", foo, Foo.class));
        sync(table.put(ctx, "bar", bar, Bar.class));
        sync(table.put(ctx, "baz", baz, Baz.class));
        sync(table.getRow("baz").delete(ctx));

        ImmutableList<WatchChange> expectedChanges = ImmutableList.of(
                new WatchChange(TABLE_NAME, "bar", ChangeType.PUT_CHANGE,
                        VomUtil.encode(bar, Bar.class), null, false, false),
                new WatchChange(TABLE_NAME, "baz", ChangeType.PUT_CHANGE,
                        VomUtil.encode(baz, Baz.class), null, false, false),
                new WatchChange(TABLE_NAME, "baz", ChangeType.DELETE_CHANGE,
                        new byte[0], null, false, false ));
        CancelableVContext ctxC = ctx.withCancel();
        Iterator<WatchChange> it = sync(db.watch(ctxC, TABLE_NAME, "b", marker)).iterator();
        for (WatchChange expected : expectedChanges) {
            assertThat(it.hasNext());
            WatchChange actual = it.next();
            assertThat(actual.getTableName()).isEqualTo(expected.getTableName());
            assertThat(actual.getRowName()).isEqualTo(expected.getRowName());
            assertThat(actual.getChangeType()).isEqualTo(expected.getChangeType());
            assertThat(actual.getVomValue()).isEqualTo(expected.getVomValue());
            assertThat(actual.isFromSync()).isEqualTo(expected.isFromSync());
            assertThat(actual.isContinued()).isEqualTo(expected.isContinued());
        }
        ctxC.cancel();
    }

    public void testDatabaseWatchWithContextCancel() throws Exception {
        final CancelableVContext cancelCtx = ctx.withCancel();
        Database db = createDatabase(createApp(createService()));
        createTable(db);

        VIterable<WatchChange> it = sync(db.watch(
                cancelCtx, TABLE_NAME, "b", sync(db.getResumeMarker(ctx))));
        new Thread(new Runnable() {
            @Override
            public void run() {
                cancelCtx.cancel();
            }
        }).start();
        assertThat(it).isEmpty();
    }

    public void testBatch() throws Exception {
        Database db = createDatabase(createApp(createService()));
        Table table = createTable(db);
        assertThat(sync(table.scan(ctx, RowRange.prefix("")))).isEmpty();

        BatchDatabase batchFoo = sync(db.beginBatch(ctx, null));
        Table batchFooTable = batchFoo.getTable(TABLE_NAME);
        assertThat(sync(batchFooTable.exists(ctx))).isTrue();
        sync(batchFooTable.put(ctx, ROW_NAME, "foo", String.class));
        // Assert that value is visible inside the batch but not outside.
        assertThat(sync(batchFooTable.get(ctx, ROW_NAME, String.class))).isEqualTo("foo");
        assertThat(sync(table.getRow(ROW_NAME).exists(ctx))).isFalse();

        BatchDatabase batchBar = sync(db.beginBatch(ctx, null));
        Table batchBarTable = batchBar.getTable(TABLE_NAME);
        assertThat(sync(batchBarTable.exists(ctx))).isTrue();
        sync(batchBarTable.put(ctx, ROW_NAME, "foo", String.class));
        // Assert that value is visible inside the batch but not outside.
        assertThat(sync(batchBarTable.get(ctx, ROW_NAME, String.class))).isEqualTo("foo");
        assertThat(sync(table.getRow(ROW_NAME).exists(ctx))).isFalse();

        sync(batchFoo.commit(ctx));
        // Assert that the value is visible outside the batch.
        assertThat(sync(table.get(ctx, ROW_NAME, String.class))).isEqualTo("foo");

        try {
            sync(batchBar.commit(ctx));
            fail("Expected batchBar.commit() to fail");
        } catch (VException e) {
            // ok
        }
    }

    public void testSyncgroup() throws Exception {
        Database db = createDatabase(createApp(createService()));
        String groupName = "test";

        // "A" creates the group.
        SyncgroupSpec spec = new SyncgroupSpec("test", allowAll,
            ImmutableList.of(new TableRow(TABLE_NAME, "")),
            ImmutableList.<String>of(), false);
        SyncgroupMemberInfo memberInfo = new SyncgroupMemberInfo();
	memberInfo.setSyncPriority((byte) 1);
        Syncgroup group = db.getSyncgroup(groupName);
        {
            sync(group.create(ctx, spec, memberInfo));
            assertThat(sync(db.listSyncgroupNames(ctx))).containsExactly(groupName);
            assertThat(sync(group.getSpec(ctx)).values()).containsExactly(spec);
            assertThat(sync(group.getMembers(ctx)).values()).containsExactly(memberInfo);
            assertThat(sync(group.join(ctx, memberInfo))).isEqualTo(spec);
        }
        // TODO(spetrovic): test leave() and destroy().

        SyncgroupSpec specRMW = new SyncgroupSpec("testRMW", allowAll,
            ImmutableList.of(new TableRow(TABLE_NAME, "")),
            ImmutableList.<String>of(), false);
        assertThat(sync(group.getSpec(ctx)).keySet()).isNotEmpty();
        String version = sync(group.getSpec(ctx)).keySet().iterator().next();
        sync(group.setSpec(ctx, specRMW, version));
        assertThat(sync(group.getSpec(ctx)).values()).containsExactly(specRMW);
        SyncgroupSpec specOverwrite = new SyncgroupSpec("testOverwrite", allowAll,
            ImmutableList.of(new TableRow(TABLE_NAME, "")),
            ImmutableList.<String>of(), false);
        sync(group.setSpec(ctx, specOverwrite, ""));
        assertThat(sync(group.getSpec(ctx)).values()).containsExactly(specOverwrite);
    }

    // TODO(spetrovic): Test Database.upgradeIfOutdated().

    public void testBlobSmall() throws Exception {
        byte[] data = new byte[]{ 1, 2, 3, 4, 5 };
        Database db = createDatabase(createApp(createService()));
        BlobWriter writer = sync(db.writeBlob(ctx, null));
        OutputStream out = sync(writer.stream(ctx));
        out.write(data);
        out.close();
        assertThat(sync(writer.size(ctx))).isEqualTo(data.length);
        sync(writer.commit(ctx));
        BlobRef ref = writer.getRef();

        BlobReader reader = db.readBlob(ctx, ref);
        byte[] actual = new byte[data.length];
        ByteStreams.readFully(sync(reader.stream(ctx, 0)), actual);
        assertThat(actual).isEqualTo(data);
    }

    public void testBlobLarge() throws Exception {
        byte[] data = new byte[1 << 17];
        for (int i = 0; i < data.length; ++i) {
            data[i] = (byte)(i & 0xFF);
        }
        Database db = createDatabase(createApp(createService()));
        BlobWriter writer = sync(db.writeBlob(ctx, null));
        OutputStream out = sync(writer.stream(ctx));
        out.write(data);
        out.close();
        assertThat(sync(writer.size(ctx))).isEqualTo(data.length);
        sync(writer.commit(ctx));
        BlobRef ref = writer.getRef();

        BlobReader reader = db.readBlob(ctx, ref);
        byte[] actual = new byte[data.length];
        ByteStreams.readFully(sync(reader.stream(ctx, 0)), actual);
        assertThat(actual).isEqualTo(data);
    }

    public void testBlobWriteResume() throws Exception {
        Database db = createDatabase(createApp(createService()));
        BlobWriter writer = sync(db.writeBlob(ctx, null));
        BlobRef ref = writer.getRef();
        byte[] data = new byte[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        {
            // Write, part 1.
            OutputStream out = sync(writer.stream(ctx));
            out.write(data, 0, data.length / 2);
            out.close();
            assertThat(sync(writer.size(ctx))).isEqualTo(data.length / 2);
        }
        {
            // Write, part 2.
            writer = sync(db.writeBlob(ctx, ref));
            assertThat(sync(writer.size(ctx))).isEqualTo(5);
            OutputStream out = sync(writer.stream(ctx));
            out.write(data, data.length / 2, data.length / 2);
            out.close();
            assertThat(sync(writer.size(ctx))).isEqualTo(data.length);
            sync(writer.commit(ctx));
        }
        // Read.
        BlobReader reader = db.readBlob(ctx, ref);
        byte[] actual = new byte[data.length];
        ByteStreams.readFully(sync(reader.stream(ctx, 0)), actual);
        assertThat(actual).isEqualTo(data);
    }

    public void testBlobWriteCommitted() throws Exception {
        byte[] data = new byte[]{ 1, 2, 3, 4, 5 };
        Database db = createDatabase(createApp(createService()));
        BlobWriter writer = sync(db.writeBlob(ctx, null));
        BlobRef ref = writer.getRef();
        OutputStream out = sync(writer.stream(ctx));
        out.write(data);
        out.close();
        assertThat(sync(writer.size(ctx))).isEqualTo(data.length);
        sync(writer.commit(ctx));

        try {
            out = sync(writer.stream(ctx));
            out.write(data);
            out.close();
            fail("write of a committed blob should fail");
        } catch (Exception e) {
            // OK
        }
        try {
            sync(writer.commit(ctx));
            fail("commit of a committed blob should fail");
        } catch (VException e) {
            // OK
        }

        BlobReader reader = db.readBlob(ctx, ref);
        byte[] actual = new byte[data.length];
        ByteStreams.readFully(sync(reader.stream(ctx, 0)), actual);
        assertThat(actual).isEqualTo(data);
    }

    public void testBlobWriteCancelable() throws Exception {
        Database db = createDatabase(createApp(createService()));
        CancelableVContext ctxC = ctx.withCancel();
        BlobWriter writer = sync(db.writeBlob(ctxC, null));
        BlobRef ref = writer.getRef();
        byte[] data = new byte[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        // Write 1st chunk.
        OutputStream out = sync(writer.stream(ctxC));
        out.write(data, 0, data.length / 2);
        ctxC.cancel();
        // Write 2nd chunk.
        try {
            out.write(data, data.length / 2, data.length / 2);
            out.close();
            fail("write on a canceled stream should fail");
        } catch (IOException e) {
            // OK
        }
    }

    public void testBlobReadUncommitted() throws Exception {
        Database db = createDatabase(createApp(createService()));
        BlobWriter writer = sync(db.writeBlob(ctx, null));
        BlobRef ref = writer.getRef();
        byte[] data = new byte[]{ 1, 2, 3, 4, 5 };
        OutputStream out = sync(writer.stream(ctx));
        out.write(data, 0, data.length);
        out.close();

        BlobReader reader = db.readBlob(ctx, ref);
        try {
            byte[] actual = new byte[data.length];
            ByteStreams.readFully(sync(reader.stream(ctx, 0)), actual);
            fail("read of an uncommitted blob should fail");
        } catch (VException | IOException e) {
            // OK
        }
        try {
            sync(reader.prefetch(ctx, 0)).iterator().next();
        } catch (VException | NoSuchElementException e) {
            // OK
        }
    }

    public void testBlobReadPrefetch() throws Exception {
        Database db = createDatabase(createApp(createService()));
        BlobWriter writer = sync(db.writeBlob(ctx, null));
        BlobRef ref = writer.getRef();
        byte[] data = new byte[]{ 1, 2, 3, 4, 5 };
        OutputStream out = sync(writer.stream(ctx));
        out.write(data, 0, data.length);
        out.close();
        sync(writer.commit(ctx));

        // Prefetch
        BlobReader reader = db.readBlob(ctx, ref);
        for (BlobFetchStatus status : sync(reader.prefetch(ctx, 0))) {}
        // Read
        byte[] actual = new byte[data.length];
        ByteStreams.readFully(sync(reader.stream(ctx, 0)), actual);
        assertThat(actual).isEqualTo(data);
    }

    public void testBlobReadClosedStream() throws Exception {
        byte[] data = new byte[]{ 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
        Database db = createDatabase(createApp(createService()));
        BlobWriter writer = sync(db.writeBlob(ctx, null));
        OutputStream out = sync(writer.stream(ctx));
        out.write(data);
        out.close();
        assertThat(sync(writer.size(ctx))).isEqualTo(data.length);
        sync(writer.commit(ctx));
        BlobRef ref = writer.getRef();

        BlobReader reader = db.readBlob(ctx, ref);
        byte[] actual = new byte[data.length / 2];
        InputStream in = sync(reader.stream(ctx, 0));
        // Read 1st chunk.
        ByteStreams.readFully(in, actual);
        assertThat(actual).isEqualTo(new byte[]{1, 2, 3, 4, 5});
        // Close the input stream.
        in.close();
        // Read 2nd chunk.
        try {
            ByteStreams.readFully(in, actual);
            fail("read of a closed stream should fail");
        } catch (IOException e) {
            // OK
        }
    }


    private SyncbaseService createService() throws Exception {
        return Syncbase.newService(serverEndpoint.name());
    }

    private SyncbaseApp createApp(SyncbaseService service) throws Exception {
        SyncbaseApp app = service.getApp(APP_NAME);
        sync(app.create(ctx, allowAll));
        return app;
    }

    private Database createDatabase(SyncbaseApp app) throws Exception {
        Database db = app.getNoSqlDatabase(DB_NAME, null);
        sync(db.create(ctx, allowAll));
        return db;
    }

    private Table createTable(Database db) throws Exception {
        Table table = db.getTable(TABLE_NAME);
        sync(table.create(ctx, allowAll));
        return table;
    }

    private static class Foo implements Serializable {
        private int i;
        private String s;

        public Foo() {
            this.i = 0;
            this.s = "";
        }

        public Foo(int i, String s) {
            this.i = i;
            this.s = s;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Foo foo = (Foo) o;

            if (i != foo.i) return false;
            return !(s != null ? !s.equals(foo.s) : foo.s != null);

        }
    }

    private static class Bar implements Serializable {
        private float f;
        private String s;

        public Bar() {
            this.f = 0f;
            this.s = "";
        }

        public Bar(float f, String s) {
            this.f = f;
            this.s = s;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Bar bar = (Bar) o;

            if (Float.compare(bar.f, f) != 0) return false;
            return !(s != null ? !s.equals(bar.s) : bar.s != null);

        }
    }

    private static class Baz implements Serializable {
        private String name;
        private boolean active;

        public Baz() {
            this.name = "";
            this.active = false;
        }

        public Baz(String name, boolean active) {
            this.name = name;
            this.active = active;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Baz baz = (Baz) o;

            if (active != baz.active) return false;
            return !(name != null ? !name.equals(baz.name) : baz.name != null);

        }
    }
}
