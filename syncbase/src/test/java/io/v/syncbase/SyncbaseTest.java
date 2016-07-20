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
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.v.syncbase.core.Permissions;
import io.v.syncbase.core.VError;
import io.v.syncbase.exception.SyncbaseException;

import static com.google.common.truth.Truth.assertThat;
import static io.v.syncbase.TestUtil.createDatabase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SyncbaseTest {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    // To run these tests from Android Studio, add the following VM option to the default JUnit
    // build configuration, via Run > Edit Configurations... > Defaults > JUnit > VM options:
    // -Djava.library.path=/Users/sadovsky/vanadium/release/java/syncbase/build/libs
    @Before
    public void setUp() throws Exception {
        TestUtil.setUpSyncbase(folder.newFolder());
    }

    @After
    public void tearDown() {
        Syncbase.shutdown();
    }

    private static Iterable<Id> getCollectionIds(Database db) throws SyncbaseException {
        List<Id> res = new ArrayList<>();
        for (Iterator<Collection> it = db.getCollections(); it.hasNext(); ) {
            res.add(it.next().getId());
        }
        return res;
    }

    private static boolean idsMatch(Iterable<Id> ids, String blessing, List<String> prefixes) {
        int i = 0;
        for (Id id : ids) {
            if (!idMatch(id, blessing, prefixes.get(i))) {
                return false;
            }
            i++;
        }
        return prefixes.size() == i; // Every id matches, and all prefixes were used.
    }

    private static boolean idMatch(Id id, String blessing, String prefix) {
        return id.getName().startsWith(prefix) && id.getBlessing().equals(blessing);
    }

    private static Iterable<Id> getSyncgroupIds(Database db) throws SyncbaseException {
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
        DatabaseHandle.CollectionOptions opts = new DatabaseHandle.CollectionOptions().
                setWithoutSyncgroup(true).setPrefix("a");

        Collection cxA = db.createCollection(opts);
        assertNotNull(cxA);
        assertTrue(idsMatch(getCollectionIds(db), Syncbase.getPersonalBlessingString(),
                ImmutableList.of("a", Syncbase.USERDATA_NAME)));

        db.createCollection(opts.setPrefix("b"));
        assertTrue(idsMatch(getCollectionIds(db), Syncbase.getPersonalBlessingString(),
                ImmutableList.of("a", "b", Syncbase.USERDATA_NAME)));

        // Note, createDatabase() sets disableSyncgroupPublishing to true, so
        // db.createCollection(opts) is still a purely local operation.
        opts = new DatabaseHandle.CollectionOptions();
        db.createCollection(opts.setPrefix("c"));
        assertTrue(idsMatch(getCollectionIds(db), Syncbase.getPersonalBlessingString(),
                ImmutableList.of("a", "b", "c", Syncbase.USERDATA_NAME)));

        Collection secondCxA = db.createCollection(opts.setPrefix("a"));
        assertFalse(cxA.getId().equals(secondCxA.getId()));
        assertTrue(idsMatch(getCollectionIds(db), Syncbase.getPersonalBlessingString(),
                ImmutableList.of("a", "a", "b", "c", Syncbase.USERDATA_NAME)));
    }

    @Test
    public void testRowCrudMethods() throws Exception {
        Database db = createDatabase();
        Collection cx = db.createCollection();
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
        Collection cxA = db.createCollection(opts.setPrefix("a"));
        Collection cxB = db.createCollection(opts.setPrefix("b"));
        Collection cxC = db.createCollection(opts.setWithoutSyncgroup(false).setPrefix("c"));
        assertNotNull(cxA);
        // Note, there's no userdata syncgroup since we set disableUserdataSyncgroup to true.
        assertTrue(idsMatch(getSyncgroupIds(db), Syncbase.getPersonalBlessingString(),
                ImmutableList.of("c")));
        db.syncgroup("sg1", ImmutableList.of(cxA));
        db.syncgroup("sg2", ImmutableList.of(cxA, cxB, cxC));
        assertThat(getSyncgroupIds(db)).containsExactly(
                new Id(Syncbase.getPersonalBlessingString(), cxC.getSyncgroup().getId().getName()),
                new Id(Syncbase.getPersonalBlessingString(), "sg1"),
                new Id(Syncbase.getPersonalBlessingString(), "sg2"));
    }

    @Test
    public void testWatch() throws Exception {
        Database db = createDatabase();
        final SettableFuture<Void> waitOnInitialState = SettableFuture.create();
        final SettableFuture<Void> waitOnChangeBatch = SettableFuture.create();
        Collection collection = db.createCollection();
        collection.put("foo", 1);
        final String collectionName = collection.getId().getName();
        db.addWatchChangeHandler(new Database.WatchChangeHandler() {
            @Override
            public void onInitialState(Iterator<WatchChange> values) {
                // TODO(razvanm): Check the entire contents of each change.
                // 1st change: the collection entity for the "c" collection.
                assertTrue(values.hasNext());
                WatchChange watchChange = (WatchChange) values.next();
                assertEquals(WatchChange.EntityType.COLLECTION, watchChange.getEntityType());
                assertEquals(WatchChange.ChangeType.PUT, watchChange.getChangeType());
                assertTrue(watchChange.getCollectionId().getName().equals(collectionName));
                // 2nd change: the row for the "foo" key.
                assertTrue(values.hasNext());
                watchChange = (WatchChange) values.next();
                assertEquals(WatchChange.EntityType.ROW, watchChange.getEntityType());
                assertEquals(WatchChange.ChangeType.PUT, watchChange.getChangeType());
                assertTrue(watchChange.getCollectionId().getName().equals(collectionName));
                assertEquals("foo", watchChange.getRowKey());
                // TODO(razvanm): Uncomment after the POJO start working.
                //assertEquals(1, watchChange.getValue());
                // 3rd change: the collection entity for the userdata collection.
                assertTrue(values.hasNext());
                watchChange = (WatchChange) values.next();
                assertEquals(WatchChange.EntityType.COLLECTION, watchChange.getEntityType());
                assertEquals(WatchChange.ChangeType.PUT, watchChange.getChangeType());
                assertTrue(watchChange.getCollectionId().getName().equals(Syncbase.USERDATA_NAME));
                // No more changes.
                assertFalse(values.hasNext());
                waitOnInitialState.set(null);
            }

            @Override
            public void onChangeBatch(Iterator<WatchChange> changes) {
                assertTrue(changes.hasNext());
                WatchChange watchChange = changes.next();
                assertEquals(WatchChange.ChangeType.DELETE, watchChange.getChangeType());
                assertTrue(watchChange.getCollectionId().getName().startsWith("c"));
                assertEquals("foo", watchChange.getRowKey());

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
    public void testWatchSpecificCollection() throws Exception {
        Database db = createDatabase();
        final SettableFuture<Void> waitOnInitialState = SettableFuture.create();
        final SettableFuture<Void> waitOnChangeBatch = SettableFuture.create();
        Collection collection = db.createCollection();
        collection.put("foo", 1);
        // Note: For ease of test writing, "gar" is a key lexicographically after "foo".
        collection.put("gar", 2);
        final String collectionName = collection.getId().getName();
        Database.AddWatchChangeHandlerOptions opts = new Database.AddWatchChangeHandlerOptions.
                Builder().setCollectionId(collection.getId()).build();
        db.addWatchChangeHandler(new Database.WatchChangeHandler() {
            @Override
            public void onInitialState(Iterator<WatchChange> values) {
                // TODO(razvanm): Check the entire contents of each change.
                // 1st change: the collection entity for the "c" collection.
                assertTrue(values.hasNext());
                WatchChange watchChange = (WatchChange) values.next();
                assertEquals(WatchChange.EntityType.COLLECTION, watchChange.getEntityType());
                assertEquals(WatchChange.ChangeType.PUT, watchChange.getChangeType());
                assertTrue(watchChange.getCollectionId().getName().equals(collectionName));
                // 2nd change: the row for the "foo" key.
                assertTrue(values.hasNext());
                watchChange = (WatchChange) values.next();
                assertEquals(WatchChange.EntityType.ROW, watchChange.getEntityType());
                assertEquals(WatchChange.ChangeType.PUT, watchChange.getChangeType());
                assertTrue(watchChange.getCollectionId().getName().equals(collectionName));
                assertEquals("foo", watchChange.getRowKey());
                // TODO(razvanm): Uncomment after the POJO start working.
                //assertEquals(1, watchChange.getValue());
                // 3rd change: the row for the "gar" key.
                assertTrue(values.hasNext());
                watchChange = (WatchChange) values.next();
                assertEquals(WatchChange.EntityType.ROW, watchChange.getEntityType());
                assertEquals(WatchChange.ChangeType.PUT, watchChange.getChangeType());
                assertTrue(watchChange.getCollectionId().getName().equals(collectionName));
                assertEquals("gar", watchChange.getRowKey());
                // TODO(razvanm): Uncomment after the POJO start working.
                //assertEquals(2, watchChange.getValue());

                // No more changes.
                assertFalse(values.hasNext());
                waitOnInitialState.set(null);
            }

            @Override
            public void onChangeBatch(Iterator<WatchChange> changes) {
                assertTrue(changes.hasNext());
                WatchChange watchChange = changes.next();
                assertEquals(WatchChange.ChangeType.DELETE, watchChange.getChangeType());
                assertTrue(watchChange.getCollectionId().getName().startsWith("c"));
                assertEquals("foo", watchChange.getRowKey());

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
        }, opts);
        waitOnInitialState.get(1, TimeUnit.SECONDS);
        collection.delete("foo");
        waitOnChangeBatch.get(1, TimeUnit.SECONDS);
    }


    @Test
    public void testWatchSpecificCollectionWithRowPrefix() throws Exception {
        Database db = createDatabase();
        final SettableFuture<Void> waitOnInitialState = SettableFuture.create();
        final SettableFuture<Void> waitOnChangeBatch = SettableFuture.create();
        Collection collection = db.createCollection();
        collection.put("foo", 1);
        collection.put("bar", 1); // not seen, due to filter
        final String collectionName = collection.getId().getName();
        Database.AddWatchChangeHandlerOptions opts = new Database.AddWatchChangeHandlerOptions.
                Builder().setCollectionId(collection.getId()).setRowKeyPrefix("f").build();
        db.addWatchChangeHandler(new Database.WatchChangeHandler() {
            @Override
            public void onInitialState(Iterator<WatchChange> values) {
                // TODO(razvanm): Check the entire contents of each change.
                // 1st change: the collection entity for the "c" collection.
                assertTrue(values.hasNext());
                WatchChange watchChange = (WatchChange) values.next();
                assertEquals(WatchChange.EntityType.COLLECTION, watchChange.getEntityType());
                assertEquals(WatchChange.ChangeType.PUT, watchChange.getChangeType());
                assertTrue(watchChange.getCollectionId().getName().equals(collectionName));
                // 2nd change: the row for the "foo" key.
                assertTrue(values.hasNext());
                watchChange = (WatchChange) values.next();
                assertEquals(WatchChange.EntityType.ROW, watchChange.getEntityType());
                assertEquals(WatchChange.ChangeType.PUT, watchChange.getChangeType());
                assertTrue(watchChange.getCollectionId().getName().equals(collectionName));
                assertEquals("foo", watchChange.getRowKey());
                // TODO(razvanm): Uncomment after the POJO start working.
                //assertEquals(1, watchChange.getValue());

                // No more changes.
                assertFalse(values.hasNext());
                waitOnInitialState.set(null);
            }

            @Override
            public void onChangeBatch(Iterator<WatchChange> changes) {
                assertTrue(changes.hasNext());
                WatchChange watchChange = changes.next();
                assertEquals(WatchChange.ChangeType.DELETE, watchChange.getChangeType());
                assertTrue(watchChange.getCollectionId().getName().startsWith("c"));
                assertEquals("foo", watchChange.getRowKey());

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
        }, opts);
        waitOnInitialState.get(1, TimeUnit.SECONDS);
        collection.delete("foo");
        collection.delete("bar"); // not noticed due to filter.
        waitOnChangeBatch.get(1, TimeUnit.SECONDS);
    }

    @Test
    public void testRunInBatch() throws Exception {
        Database db = createDatabase();

        // We need a box class to store the id of the created collection since we want to refer to
        // it after the batch is over.
        class TestOperation implements Database.BatchOperation {
            private Id id;

            @Override
            public void run(BatchDatabase db) {
                try {
                    DatabaseHandle.CollectionOptions opts = new DatabaseHandle.CollectionOptions()
                            .setWithoutSyncgroup(true);
                    Collection c = db.createCollection(opts);
                    c.put("foo", 10);
                    id = c.getId();
                } catch (SyncbaseException e) {
                    e.printStackTrace();
                    fail(e.toString());
                }
            }
        }

        TestOperation op = new TestOperation();
        db.runInBatch(op);
        assertEquals(db.getCollection(op.id).get("foo", Integer.class), Integer.valueOf(10));
    }

    @Test
    public void testSyncgroupInviteUsers() throws Exception {
        Database db = createDatabase();
        Collection collection = db.createCollection();
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

    @Test
    public void unsupportedAuthenticationProvider() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Unsupported provider: bogusProvider");

        Syncbase.login("", "bogusProvider", new Syncbase.LoginCallback() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(Throwable e) {
            }
        });
    }

    @Test
    public void testAdvertiseInNeighborhood() throws Exception {
        TestUtil.createDatabase(); // This is meant to force the test login.

        assertFalse(Syncbase.isAdvertisingLoggedInUserInNeighborhood());
        Syncbase.advertiseLoggedInUserInNeighborhood();
        assertTrue(Syncbase.isAdvertisingLoggedInUserInNeighborhood());
        Syncbase.stopAdvertisingLoggedInUserInNeighborhood();
        assertFalse(Syncbase.isAdvertisingLoggedInUserInNeighborhood());
    }

    @Test
    public void testAdvertiseInNeighborhoodNotLoggedIn() {
        try {
            Syncbase.advertiseLoggedInUserInNeighborhood();
            fail("should throw because the user isn't logged in");
        } catch (SyncbaseException e) {
            // We expect the advertise attempt to throw.
        }
    }

    @Test
    public void testScanInNeighborhood() throws Exception {
        TestUtil.createDatabase(); // This is meant to force the test login.

        Syncbase.ScanNeighborhoodForUsersCallback cb =
                new Syncbase.ScanNeighborhoodForUsersCallback() {
            @Override
            public void onFound(User user) {}

            @Override
            public void onLost(User user) {}

            @Override
            public void onError(Throwable e) {
                fail("should not throw an error");
            }
        };
        // Test that this doesn't error. A more thorough test would check that the callback fires,
        // but that would require multiple devices, so it has not been done here.
        Syncbase.removeAllScansForUsersInNeighborhood();
        Syncbase.addScanForUsersInNeighborhood(cb);
        Syncbase.removeScanForUsersInNeighborhood(cb);
        Syncbase.removeAllScansForUsersInNeighborhood();

        try {
            SettableFuture.create().get(1, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            // It's okay for this to time out. We just need to ensure the error callback has
            // time to be called.
        }
    }

    @Test
    public void testScanInNeighborhoodNotLoggedIn() {
        final SettableFuture<Boolean> errored = SettableFuture.create();
        Syncbase.ScanNeighborhoodForUsersCallback cb =
                new Syncbase.ScanNeighborhoodForUsersCallback() {
                    @Override
                    public void onFound(User user) {}

                    @Override
                    public void onLost(User user) {}

                    @Override
                    public void onError(Throwable e) {
                        // This is expected to be called.
                        assertNotNull("should error", e);
                        errored.set(true);
                    }
                };
        // Force an error because the user was not logged in.
        Syncbase.addScanForUsersInNeighborhood(cb);

        // Give the callback time to be called.
        try {
            assertTrue(errored.get(1, TimeUnit.SECONDS));
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            fail("should have been able to get successfully");
        }
    }
}
