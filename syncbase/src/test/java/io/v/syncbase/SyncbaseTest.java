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
import java.util.concurrent.TimeUnit;

import io.v.syncbase.core.VError;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SyncbaseTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

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
                assertTrue(values.hasNext());
                WatchChange watchChange = (WatchChange)values.next();
                assertEquals(WatchChange.ChangeType.PUT, watchChange.getChangeType());
                // TODO(razvanm): Uncomment after the POJO start working.
                //assertEquals(1, watchChange.getValue());
                assertFalse(values.hasNext());
                waitOnInitialState.set(null);
            }

            @Override
            public void onChangeBatch(Iterator<WatchChange> changes) {
                assertTrue(changes.hasNext());
                WatchChange watchChange = (WatchChange)changes.next();
                assertEquals(WatchChange.ChangeType.DELETE, watchChange.getChangeType());
                // TODO(razvanm): Uncomment after the POJO start working.
                //assertEquals(1, watchChange.getValue());
                assertFalse(changes.hasNext());
                waitOnChangeBatch.set(null);
            }

            @Override
            public void onError(Throwable e) {
                VError vError = (VError)e;
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
        }, new Database.BatchOptions());
        assertEquals(db.collection("c").get("foo", Integer.class), Integer.valueOf(10));
    }
}