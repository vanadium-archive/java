// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.SettableFuture;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.v.syncbase.core.Permissions;
import io.v.syncbase.core.VError;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SyncbaseTest {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    // To run these tests from Android Studio, add the following VM option to the default JUnit
    // build configuration, via Run > Edit Configurations... > Defaults > JUnit > VM options:
    // -Djava.library.path=/Users/sadovsky/vanadium/release/java/syncbase/build/libs
    @Before
    public void setUp() throws Exception {
        Syncbase.Options opts = new Syncbase.Options();
        opts.rootDir = folder.newFolder().getAbsolutePath();
        opts.disableUserdataSyncgroup = true;
        opts.disableSyncgroupPublishing = true;
        opts.testLogin = true;
        // Unlike Android apps, the test doesn't have a looper/handler, so use a different executor.
        opts.callbackExecutor = Executors.newCachedThreadPool();
        Syncbase.init(opts);
    }

    @After
    public void tearDown() {
        Syncbase.shutdown();
    }

    private Database createDatabase() throws Exception {
        final SettableFuture<Void> future = SettableFuture.create();

        Syncbase.login("", "", new Syncbase.LoginCallback() {
            @Override
            public void onSuccess() {
                future.set(null);
            }

            @Override
            public void onError(Throwable e) {
                future.setException(e);
            }
        });

        future.get(5, TimeUnit.SECONDS);
        return Syncbase.database();
    }

    private static Iterable<Id> getCollectionIds(Database db) throws VError {
        List<Id> res = new ArrayList<>();
        for (Iterator<Collection> it = db.getCollections(); it.hasNext(); ) {
            res.add(it.next().getId());
        }
        return res;
    }

    private static Iterable<Id> getSyncgroupIds(Database db) throws VError {
        List<Id> res = new ArrayList<>();
        for (Iterator<Syncgroup> it = db.getSyncgroups(); it.hasNext(); ) {
            res.add(it.next().getId());
        }
        return res;
    }

    @Test
    public void testCreateDatabase() throws Exception {
        createDatabase();
    }

    @Test
    public void testCreateAndGetCollections() throws Exception {
        Database db = createDatabase();
        assertNotNull(db);
        DatabaseHandle.CollectionOptions opts = new DatabaseHandle.CollectionOptions();
        opts.withoutSyncgroup = true;
        Collection cxA = db.collection("a", opts);
        assertNotNull(cxA);
        // TODO(sadovsky): Should we omit the userdata collection?
        assertThat(getCollectionIds(db)).containsExactly(
                new Id(Syncbase.getPersonalBlessingString(), "a"),
                new Id(Syncbase.getPersonalBlessingString(), "userdata__"));
        db.collection("b", opts);
        assertThat(getCollectionIds(db)).containsExactly(
                new Id(Syncbase.getPersonalBlessingString(), "a"),
                new Id(Syncbase.getPersonalBlessingString(), "b"),
                new Id(Syncbase.getPersonalBlessingString(), "userdata__"));
        // Note, createDatabase() sets disableSyncgroupPublishing to true, so db.collection(name) is
        // a purely local operation.
        db.collection("c");
        assertThat(getCollectionIds(db)).containsExactly(
                new Id(Syncbase.getPersonalBlessingString(), "a"),
                new Id(Syncbase.getPersonalBlessingString(), "b"),
                new Id(Syncbase.getPersonalBlessingString(), "c"),
                new Id(Syncbase.getPersonalBlessingString(), "userdata__"));
        Collection secondCxA = db.collection("a", opts);
        assertEquals(cxA.getId(), secondCxA.getId());
    }

    @Test
    public void testRowCrudMethods() throws Exception {
        Database db = createDatabase();
        Collection cx = db.collection("cx");
        assertNotNull(cx);
        assertFalse(cx.exists("foo"));
        assertEquals(cx.get("foo", String.class), null);
        cx.put("foo", "bar");
        assertTrue(cx.exists("foo"));
        assertEquals(cx.get("foo", String.class), "bar");
        cx.put("foo", "baz");
        assertTrue(cx.exists("foo"));
        assertEquals(cx.get("foo", String.class), "baz");
        cx.delete("foo");
        assertFalse(cx.exists("foo"));
        assertEquals(cx.get("foo", String.class), null);
        cx.put("foo", 5);
        assertEquals(cx.get("foo", Integer.class), Integer.valueOf(5));

        // TODO(razvanm): Figure out a way to get the POJOs to work.
//        // This time, with a POJO.
//        class MyObject {
//            String str;
//            int num;
//        }
//        MyObject putObj = new MyObject();
//        putObj.str = "hello";
//        putObj.num = 7;
//        cx.put("foo", putObj);
//        MyObject getObj = cx.get("foo", MyObject.class);
//        assertEquals(putObj.str, getObj.str);
//        assertEquals(putObj.num, getObj.num);
    }

    @Test
    public void testCreateAndGetSyncgroups() throws Exception {
        Database db = createDatabase();
        DatabaseHandle.CollectionOptions opts = new DatabaseHandle.CollectionOptions();
        opts.withoutSyncgroup = true;
        Collection cxA = db.collection("a", opts);
        Collection cxB = db.collection("b", opts);
        Collection cxC = db.collection("c");
        assertNotNull(cxA);
        // Note, there's no userdata syncgroup since we set disableUserdataSyncgroup to true.
        assertThat(getSyncgroupIds(db)).containsExactly(
                new Id(Syncbase.getPersonalBlessingString(), "c"));
        db.syncgroup("sg1", ImmutableList.of(cxA));
        db.syncgroup("sg2", ImmutableList.of(cxA, cxB, cxC));
        assertThat(getSyncgroupIds(db)).containsExactly(
                new Id(Syncbase.getPersonalBlessingString(), "c"),
                new Id(Syncbase.getPersonalBlessingString(), "sg1"),
                new Id(Syncbase.getPersonalBlessingString(), "sg2"));
    }

    @Test
    public void testWatch() throws Exception {
        Database db = createDatabase();
        final SettableFuture<Void> waitOnInitialState = SettableFuture.create();
        final SettableFuture<Void> waitOnChangeBatch = SettableFuture.create();
        Collection collection = db.collection("c");
        collection.put("foo", 1);
        db.addWatchChangeHandler(new Database.WatchChangeHandler() {
            @Override
            public void onInitialState(Iterator<WatchChange> values) {
                // TODO(razvanm): Check the entire contents of each change.
                // 1st change: the collection entity for the "c" collection.
                assertTrue(values.hasNext());
                WatchChange watchChange = (WatchChange) values.next();
                assertEquals(WatchChange.EntityType.COLLECTION, watchChange.getEntityType());
                assertEquals(WatchChange.ChangeType.PUT, watchChange.getChangeType());
                assertEquals("c", watchChange.getCollectionId().getName());
                // 2nd change: the row for the "foo" key.
                assertTrue(values.hasNext());
                watchChange = (WatchChange) values.next();
                assertEquals(WatchChange.EntityType.ROW, watchChange.getEntityType());
                assertEquals(WatchChange.ChangeType.PUT, watchChange.getChangeType());
                assertEquals("c", watchChange.getCollectionId().getName());
                assertEquals("foo", watchChange.getRowKey());
                // TODO(razvanm): Uncomment after the POJO start working.
                //assertEquals(1, watchChange.getValue());
                // 3rd change: the collection entity for the userdata collection.
                assertTrue(values.hasNext());
                watchChange = (WatchChange) values.next();
                assertEquals(WatchChange.EntityType.COLLECTION, watchChange.getEntityType());
                assertEquals(WatchChange.ChangeType.PUT, watchChange.getChangeType());
                // 4th change: the userdata collection has a row for "c"'s syncgroup.
                assertTrue(values.hasNext());
                watchChange = (WatchChange) values.next();
                assertEquals(WatchChange.EntityType.ROW, watchChange.getEntityType());
                assertEquals(WatchChange.ChangeType.PUT, watchChange.getChangeType());
                assertEquals("userdata__", watchChange.getCollectionId().getName());
                assertTrue(watchChange.getRowKey().endsWith("c"));
                waitOnInitialState.set(null);
            }

            @Override
            public void onChangeBatch(Iterator<WatchChange> changes) {
                assertTrue(changes.hasNext());
                WatchChange watchChange = changes.next();
                assertEquals(WatchChange.ChangeType.DELETE, watchChange.getChangeType());
                // TODO(razvanm): Uncomment after the POJO start working.
                //assertEquals(1, watchChange.getValue());
                assertFalse(changes.hasNext());
                waitOnChangeBatch.set(null);
            }

            @Override
            public void onError(Throwable e) {
                VError vError = (VError) e;
                assertEquals("v.io/v23/verror.Unknown", vError.id);
                assertEquals("context canceled", vError.message);
                assertEquals(0, vError.actionCode);
            }
        });
        waitOnInitialState.get(1, TimeUnit.SECONDS);
        collection.delete("foo");
        waitOnChangeBatch.get(1, TimeUnit.SECONDS);
    }

    @Test
    public void testRunInBatch() throws Exception {
        Database db = createDatabase();
        db.runInBatch(new Database.BatchOperation() {
            @Override
            public void run(BatchDatabase db) {
                try {
                    DatabaseHandle.CollectionOptions opts = new DatabaseHandle.CollectionOptions()
                            .setWithoutSyncgroup(true);
                    db.collection("c", opts).put("foo", 10);
                } catch (VError vError) {
                    vError.printStackTrace();
                    fail(vError.toString());
                }
            }
        });
        assertEquals(db.collection("c").get("foo", Integer.class), Integer.valueOf(10));
    }

    @Test
    public void testSyncgroupInviteUsers() throws Exception {
        Database db = createDatabase();
        Collection collection = db.collection("c");
        Syncgroup sg = collection.getSyncgroup();

        User alice = new User("alice");
        User bob = new User("bob");
        User carol = new User("carol");

        // First let us confirm that alice, bob, and carol have no access at all.
        AccessList acl0 = sg.getAccessList();
        assertNull(acl0.getAccessLevelForUser(alice));
        assertNull(acl0.getAccessLevelForUser(bob));
        assertNull(acl0.getAccessLevelForUser(carol));

        // Alice can read now.
        sg.inviteUser(new User("alice"), AccessList.AccessLevel.READ);
        AccessList acl1 = sg.getAccessList();
        assertEquals(acl1.getAccessLevelForUser(alice), AccessList.AccessLevel.READ);
        assertNull(acl1.getAccessLevelForUser(bob));
        assertNull(acl1.getAccessLevelForUser(carol));

        // Bob can both read and write now.
        sg.inviteUser(new User("bob"), AccessList.AccessLevel.READ_WRITE);
        AccessList acl2 = sg.getAccessList();
        assertEquals(acl2.getAccessLevelForUser(alice), AccessList.AccessLevel.READ);
        assertEquals(acl2.getAccessLevelForUser(bob), AccessList.AccessLevel.READ_WRITE);
        assertNull(acl2.getAccessLevelForUser(carol));

        // Alice and Carol get full access now. (Tests overwrite and multiple invites.)
        sg.inviteUsers(ImmutableList.of(new User("alice"), new User("carol")),
                AccessList.AccessLevel.READ_WRITE_ADMIN);
        AccessList acl3 = sg.getAccessList();
        assertEquals(acl3.getAccessLevelForUser(alice), AccessList.AccessLevel.READ_WRITE_ADMIN);
        assertEquals(acl3.getAccessLevelForUser(bob), AccessList.AccessLevel.READ_WRITE);
        assertEquals(acl3.getAccessLevelForUser(carol), AccessList.AccessLevel.READ_WRITE_ADMIN);
    }

    @Test
    public void testAccessListParsing() throws Exception {
        // TODO(alexfandrianto): Test the test constants. They have "...", so we can't do it yet.

        // Let's give some arbitrary permissions to alice and bob.
        Permissions aliceBobAdmins = new Permissions(("{\"Admin\":{\"In\":[\"dev.v" +
                ".io:o:app:alice\",\"dev.v.io:o:app:bob\"]},\"Write\":{\"In\":[\"dev.v" +
                ".io:o:app:alice\",\"dev.v.io:o:app:bob\"]},\"Read\":{\"In\":[\"dev.v" +
                ".io:o:app:alice\",\"dev.v.io:o:app:bob\"]}}").getBytes());
        new AccessList(aliceBobAdmins);

        // NOT OK: Admin + !writer
        try {
            Permissions aliceBobAdminsButAliceNotWriter = new Permissions(
                    ("{\"Admin\":{\"In\":[\"dev.v.io:o:app:alice\",\"dev.v.io:o:app:bob\"]}," +
                            "\"Write\":{\"In\":[\"dev.v.io:o:app:bob\"]},\"Read\":{\"In\":[\"dev" +
                            ".v.io:o:app:alice\",\"dev.v.io:o:app:bob\"]}}")
                            .getBytes());
            new AccessList(aliceBobAdminsButAliceNotWriter);
            fail("Should have errored because Alice is an admin but not a writer.");
        } catch (IllegalArgumentException e) {
            // This is supposed to fail.
        }

        // NOT OK: Admin + Writer + !Reader
        try {
            Permissions aliceBobWritersButAliceNotReader = new Permissions(
                    ("{\"Admin\":{\"In\":[\"dev.v.io:o:app:bob\"]}," +
                            "\"Write\":{\"In\":[\"dev.v.io:o:app:alice\",\"dev.v" +
                            ".io:o:app:bob\"]}," +
                            "\"Read\":{\"In\":[\"dev.v.io:o:app:bob\"]}}").getBytes());
            new AccessList(aliceBobWritersButAliceNotReader);
            fail("Should have errored because Alice is a writer but not a reader.");
        } catch (IllegalArgumentException e) {
            // This is supposed to fail.
        }
    }
}