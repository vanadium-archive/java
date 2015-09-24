// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import io.v.impl.google.naming.NamingUtil;
import io.v.impl.google.services.syncbase.SyncbaseServer;
import io.v.v23.naming.Endpoint;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.services.syncbase.nosql.KeyValue;
import io.v.v23.services.syncbase.nosql.SyncGroupMemberInfo;
import io.v.v23.services.syncbase.nosql.SyncGroupSpec;
import io.v.v23.syncbase.nosql.BatchDatabase;
import io.v.v23.syncbase.nosql.ChangeType;
import io.v.v23.syncbase.nosql.Database;
import io.v.v23.syncbase.nosql.ResultStream;
import io.v.v23.syncbase.nosql.Row;
import io.v.v23.syncbase.nosql.RowRange;
import io.v.v23.syncbase.nosql.Stream;
import io.v.v23.syncbase.nosql.SyncGroup;
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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

import static com.google.common.truth.Truth.assertThat;

/**
 * Client-server syncbase tests.
 */
public class SyncbaseTest extends TestCase {
    private static final String APP_NAME = "app";
    private static final String DB_NAME = "db";
    private static final String TABLE_NAME = "table";
    private static final String ROW_NAME = "row";

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
        assertThat(service.listApps(ctx)).isEmpty();
    }

    public void testApp() throws Exception {
        SyncbaseService service = createService();
        SyncbaseApp app = service.getApp(APP_NAME);
        assertThat(app).isNotNull();
        assertThat(app.name()).isEqualTo(APP_NAME);
        assertThat(app.fullName()).is(NamingUtil.join(serverEndpoint.name(), APP_NAME));
        assertThat(app.exists(ctx)).isFalse();
        assertThat(service.listApps(ctx)).isEmpty();
        app.create(ctx, allowAll);
        assertThat(app.exists(ctx)).isTrue();
        assertThat(Arrays.asList(service.listApps(ctx))).containsExactly(app.name());
        assertThat(app.listDatabases(ctx)).isEmpty();
        app.destroy(ctx);
        assertThat(app.exists(ctx)).isFalse();
        assertThat(service.listApps(ctx)).isEmpty();
    }

    public void testDatabase() throws Exception {
        SyncbaseApp app = createApp(createService());
        assertThat(app).isNotNull();
        Database db = app.getNoSqlDatabase("db", null);
        assertThat(db).isNotNull();
        assertThat(db.name()).isEqualTo(DB_NAME);
        assertThat(db.fullName()).isEqualTo(
                NamingUtil.join(serverEndpoint.name(), APP_NAME, DB_NAME));
        assertThat(db.exists(ctx)).isFalse();
        assertThat(app.listDatabases(ctx)).isEmpty();
        db.create(ctx, allowAll);
        assertThat(db.exists(ctx)).isTrue();
        assertThat(Arrays.asList(app.listDatabases(ctx))).containsExactly(db.name());
        assertThat(db.listTables(ctx)).isEmpty();
        db.destroy(ctx);
        assertThat(db.exists(ctx)).isFalse();
        assertThat(app.listDatabases(ctx)).isEmpty();
    }

    public void testTable() throws Exception {
        Database db = createDatabase(createApp(createService()));
        assertThat(db).isNotNull();
        Table table = db.getTable(TABLE_NAME);
        assertThat(table).isNotNull();
        assertThat(table.name()).isEqualTo(TABLE_NAME);
        assertThat(table.fullName()).isEqualTo(NamingUtil.join(serverEndpoint.name(),
                APP_NAME, DB_NAME, TABLE_NAME));
        assertThat(table.exists(ctx)).isFalse();
        assertThat(db.listTables(ctx)).isEmpty();
        table.create(ctx, allowAll);
        assertThat(table.exists(ctx)).isTrue();
        assertThat(Arrays.asList(db.listTables(ctx))).containsExactly(TABLE_NAME);

        assertThat(table.getRow("row1").exists(ctx)).isFalse();
        table.put(ctx, "row1", "value1", String.class);
        assertThat(table.getRow("row1").exists(ctx)).isTrue();
        assertThat(table.get(ctx, "row1", String.class)).isEqualTo("value1");
        table.delete(ctx, "row1");
        assertThat(table.getRow("row1").exists(ctx)).isFalse();
        table.put(ctx, "row1", "value1", String.class);
        table.put(ctx, "row2", "value2", String.class);
        assertThat(table.getRow("row1").exists(ctx)).isTrue();
        assertThat(table.getRow("row2").exists(ctx)).isTrue();
        assertThat(table.get(ctx, "row1", String.class)).isEqualTo("value1");
        assertThat(table.get(ctx, "row2", String.class)).isEqualTo("value2");
        assertThat(table.scan(ctx, RowRange.range("row1", "row3"))).containsExactly(
                new KeyValue("row1", VomUtil.encode("value1", String.class)),
                new KeyValue("row2", VomUtil.encode("value2", String.class)));
        table.deleteRange(ctx, RowRange.range("row1", "row3"));
        assertThat(table.getRow("row1").exists(ctx)).isFalse();
        assertThat(table.getRow("row2").exists(ctx)).isFalse();

        table.destroy(ctx);
        assertThat(table.exists(ctx)).isFalse();
        assertThat(db.listTables(ctx)).isEmpty();
    }

    public void testRow() throws Exception {
        Table table = createTable(createDatabase(createApp(createService())));
        Row row = table.getRow(ROW_NAME);
        assertThat(row).isNotNull();
        assertThat(row.key()).isEqualTo(ROW_NAME);
        assertThat(row.fullName()).isEqualTo(NamingUtil.join(serverEndpoint.name(), APP_NAME,
                DB_NAME, TABLE_NAME, ROW_NAME));
        assertThat(row.exists(ctx)).isFalse();
        row.put(ctx, "value", String.class);
        assertThat(row.exists(ctx)).isTrue();
        assertThat(row.get(ctx, String.class)).isEqualTo("value");
        assertThat(table.get(ctx, ROW_NAME, String.class)).isEqualTo("value");
        row.delete(ctx);
        assertThat(row.exists(ctx)).isFalse();
        table.put(ctx, ROW_NAME, "value", String.class);
        assertThat(row.exists(ctx)).isTrue();
        assertThat(row.get(ctx, String.class)).isEqualTo("value");
        assertThat(table.get(ctx, ROW_NAME, String.class)).isEqualTo("value");
    }

    public void testDatabaseExec() throws Exception {
        Database db = createDatabase(createApp(createService()));
        Table table = createTable(db);
        Foo foo = new Foo(4, "f");
        Bar bar = new Bar(0.5f, "b");
        Baz baz = new Baz("John Doe", true);

        table.put(ctx, "foo", foo, Foo.class);
        table.put(ctx, "bar", bar, Bar.class);
        table.put(ctx, "baz", baz, Baz.class);

        {
            ResultStream stream = db.exec(ctx,
                    "select k, v.Name from " + TABLE_NAME + " where Type(v) like \"%Baz\"");
            assertThat(stream.columnNames()).containsExactly("k", "v.Name");
            assertThat(stream).containsExactly(ImmutableList.of(
                    new VdlAny(String.class, "baz"), new VdlAny(String.class, baz.name)));
        }
        {
            ResultStream stream = db.exec(ctx, "select k, v from " + TABLE_NAME);
            assertThat(stream.columnNames()).containsExactly("k", "v");
            assertThat(stream).containsExactly(
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

        Stream<WatchChange> watchStream = db.watch(ctx, TABLE_NAME, "b", db.getResumeMarker(ctx));
        table.put(ctx, "foo", foo, Foo.class);
        table.put(ctx, "bar", bar, Bar.class);
        table.put(ctx, "baz", baz, Baz.class);
        table.getRow("baz").delete(ctx);

        ImmutableList<WatchChange> expectedChanges = ImmutableList.of(
                new WatchChange(TABLE_NAME, "bar", ChangeType.PUT_CHANGE,
                        VomUtil.encode(bar, Bar.class), null, false, false),
                new WatchChange(TABLE_NAME, "baz", ChangeType.PUT_CHANGE,
                        VomUtil.encode(baz, Baz.class), null, false, false),
                new WatchChange(TABLE_NAME, "baz", ChangeType.DELETE_CHANGE,
                        new byte[0], null, false, false ));
        Iterator<WatchChange> iterator = watchStream.iterator();
        for (WatchChange expected : expectedChanges) {
            assertThat(iterator.hasNext());
            WatchChange actual = iterator.next();
            assertThat(actual.getTableName()).isEqualTo(expected.getTableName());
            assertThat(actual.getRowName()).isEqualTo(expected.getRowName());
            assertThat(actual.getChangeType()).isEqualTo(expected.getChangeType());
            assertThat(actual.getVomValue()).isEqualTo(expected.getVomValue());
            assertThat(actual.isFromSync()).isEqualTo(expected.isFromSync());
            assertThat(actual.isContinued()).isEqualTo(expected.isContinued());
        }
        watchStream.cancel();
    }

    public void testBatch() throws Exception {
        Database db = createDatabase(createApp(createService()));
        Table table = createTable(db);
        assertThat(table.scan(ctx, RowRange.prefix(""))).isEmpty();

        BatchDatabase batchFoo = db.beginBatch(ctx, null);
        Table batchFooTable = batchFoo.getTable(TABLE_NAME);
        assertThat(batchFooTable.exists(ctx)).isTrue();
        batchFooTable.put(ctx, ROW_NAME, "foo", String.class);
        // Assert that value is visible inside the batch but not outside.
        assertThat(batchFooTable.get(ctx, ROW_NAME, String.class)).isEqualTo("foo");
        assertThat(table.getRow(ROW_NAME).exists(ctx)).isFalse();

        BatchDatabase batchBar = db.beginBatch(ctx, null);
        Table batchBarTable = batchBar.getTable(TABLE_NAME);
        assertThat(batchBarTable.exists(ctx)).isTrue();
        batchBarTable.put(ctx, ROW_NAME, "foo", String.class);
        // Assert that value is visible inside the batch but not outside.
        assertThat(batchBarTable.get(ctx, ROW_NAME, String.class)).isEqualTo("foo");
        assertThat(table.getRow(ROW_NAME).exists(ctx)).isFalse();

        batchFoo.commit(ctx);
        // Assert that the value is visible outside the batch.
        assertThat(table.get(ctx, ROW_NAME, String.class)).isEqualTo("foo");

        try {
            batchBar.commit(ctx);
            fail("Expected batchBar.commit() to fail");
        } catch (VException e) {
            // ok
        }
    }

    public void testSyncGroup() throws Exception {
        Database db = createDatabase(createApp(createService()));
        String groupName = "test";

        // "A" creates the group.
        SyncGroupSpec spec = new SyncGroupSpec("test", allowAll,
                ImmutableList.of(TABLE_NAME + "/"), ImmutableList.<String>of(), false);
        SyncGroupMemberInfo memberInfo = new SyncGroupMemberInfo((byte) 1);
        SyncGroup group = db.getSyncGroup(groupName);
        {
            group.create(ctx, spec, memberInfo);
            assertThat(Arrays.asList(db.listSyncGroupNames(ctx))).containsExactly(groupName);
            assertThat(group.getSpec(ctx).values()).containsExactly(spec);
            assertThat(group.getMembers(ctx).values()).containsExactly(memberInfo);
            assertThat(group.join(ctx, memberInfo)).isEqualTo(spec);
        }
        // TODO(spetrovic): test leave() and destroy().

        SyncGroupSpec specRMW = new SyncGroupSpec("testRMW", allowAll,
                ImmutableList.of(TABLE_NAME + "/"), ImmutableList.<String>of(), false);
        assertThat(group.getSpec(ctx).keySet()).isNotEmpty();
        String version = group.getSpec(ctx).keySet().iterator().next();
        group.setSpec(ctx, specRMW, version);
        assertThat(group.getSpec(ctx).values()).containsExactly(specRMW);
        SyncGroupSpec specOverwrite = new SyncGroupSpec("testOverwrite", allowAll,
                ImmutableList.of(TABLE_NAME + "/"), ImmutableList.<String>of(), false);
        group.setSpec(ctx, specOverwrite, "");
        assertThat(group.getSpec(ctx).values()).containsExactly(specOverwrite);
    }

    // TODO(spetrovic): Test Database.upgradeIfOutdated().

    private SyncbaseService createService() throws Exception {
        return Syncbase.newService(serverEndpoint.name());
    }

    private SyncbaseApp createApp(SyncbaseService service) throws Exception {
        SyncbaseApp app = service.getApp(APP_NAME);
        app.create(ctx, allowAll);
        return app;
    }

    private Database createDatabase(SyncbaseApp app) throws Exception {
        Database db = app.getNoSqlDatabase(DB_NAME, null);
        db.create(ctx, allowAll);
        return db;
    }

    private Table createTable(Database db) throws Exception {
        Table table = db.getTable(TABLE_NAME);
        table.create(ctx, allowAll);
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
