// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

import com.google.common.util.concurrent.SettableFuture;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.v.syncbase.core.Id;
import io.v.syncbase.core.KeyValue;
import io.v.syncbase.core.Permissions;
import io.v.syncbase.core.VError;

import static io.v.syncbase.core.TestConstants.anyCollectionPermissions;
import static io.v.syncbase.core.TestConstants.anyDbPermissions;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CollectionTest {
    @ClassRule
    public static final TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void setUp() throws Exception {
        System.loadLibrary("syncbase");
        Service.Init(folder.newFolder().getAbsolutePath(), true, 0);
        Service.Login("", "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Service.Shutdown();
    }

    @Test
    public void createCollection() {
        Id dbId = new Id("idp:a:angrybirds", "create_collection");
        Id collectionId1 = new Id("...", "collection1");
        Id collectionId2 = new Id("...", "collection2");
        try {
            Database.Create(dbId.encode(), anyDbPermissions());
            String batchHandle = Database.BeginBatch(dbId.encode(), null);

            String name1 = Util.NamingJoin(Arrays.asList(dbId.encode(), collectionId1.encode()));
            Collection.Create(name1, batchHandle, anyCollectionPermissions());

            String name2 = Util.NamingJoin(Arrays.asList(dbId.encode(), collectionId2.encode()));
            Collection.Create(name2, batchHandle, anyCollectionPermissions());

            List<Id> collections = Database.ListCollections(dbId.encode(), batchHandle);
            assertNotNull(collections);
            assertEquals(2, collections.size());
            assertEquals(collectionId1.encode(), collections.get(0).encode());
            assertEquals(collectionId2.encode(), collections.get(1).encode());
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }

    @Test
    public void destroyCollection() {
        Id dbId = new Id("idp:a:angrybirds", "destroy_collection");
        String dbName = dbId.encode();
        Id collectionId = new Id("...", "collection");
        String collectionName = Util.NamingJoin(Arrays.asList(dbName, collectionId.encode()));
        try {
            Database.Create(dbName, anyDbPermissions());
            String batchHandle = Database.BeginBatch(dbId.encode(), null);
            Collection.Create(collectionName, batchHandle, anyCollectionPermissions());
            Database.Commit(dbName, batchHandle);
            batchHandle = Database.BeginBatch(dbName, null);
            Collection.Destroy(collectionName, batchHandle);
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }

    @Test
    public void existsCollection() {
        Id dbId = new Id("idp:a:angrybirds", "exists_collection");
        String dbName = dbId.encode();
        Id collectionId1 = new Id("...", "collection1");
        String collectionName1 = Util.NamingJoin(Arrays.asList(dbName, collectionId1.encode()));
        Id collectionId2 = new Id("...", "collection2");
        String collectionName2 = Util.NamingJoin(Arrays.asList(dbName, collectionId2.encode()));
        try {
            Database.Create(dbName, anyDbPermissions());
            String batchHandle = Database.BeginBatch(dbName, null);
            Collection.Create(collectionName1, batchHandle, anyCollectionPermissions());

            // We have not committed the batch yet so exists() should fail.
            assertFalse(Collection.Exists(collectionName1, Database.BeginBatch(dbName, null)));

            // But from the point of view of the batch, collection1 does exist.
            assertTrue(Collection.Exists(collectionName1, batchHandle));

            Database.Commit(dbName, batchHandle);
            batchHandle = Database.BeginBatch(dbName, null);
            assertTrue(Collection.Exists(collectionName1, batchHandle));
            assertFalse(Collection.Exists(collectionName2, batchHandle));
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }

    @Test
    public void permissions() {
        Id dbId = new Id("idp:a:angrybirds", "permissions_collection");
        String dbName = dbId.encode();
        Id collectionId = new Id("...", "collection");
        String collectionName = Util.NamingJoin(Arrays.asList(dbName, collectionId.encode()));
        try {
            Database.Create(dbName, anyDbPermissions());
            String batchHandle = Database.BeginBatch(dbId.encode(), null);
            Collection.Create(collectionName, batchHandle, anyCollectionPermissions());
            Permissions permissions = Collection.GetPermissions(collectionName, batchHandle);
            assertNotNull(permissions);
            String json = new String(permissions.json);
            assertTrue(json.contains("Admin"));
            Database.Commit(dbName, batchHandle);

            batchHandle = Database.BeginBatch(dbId.encode(), null);
            Collection.SetPermissions(collectionName, batchHandle, permissions);
            Database.Commit(dbName, batchHandle);
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }

    @Test
    public void deleteRangeCollection() {
        Id dbId = new Id("idp:a:angrybirds", "delete_range_collection");
        String dbName = dbId.encode();
        Id collectionId = new Id("...", "collection");
        String collectionName = Util.NamingJoin(Arrays.asList(dbName, collectionId.encode()));
        String keyName = Util.NamingJoin(Arrays.asList(collectionName, "key"));
        // Reference: release/go/src/v.io/v23/vom/testdata/data81/vomdata.vdl
        byte[] vomValue = {(byte)0x81, 0x06, 0x03, 'a', 'b', 'c'};
        try {
            Database.Create(dbName, anyDbPermissions());
            String batchHandle = Database.BeginBatch(dbId.encode(), null);
            Collection.Create(collectionName, batchHandle, anyCollectionPermissions());
            Row.Put(keyName, batchHandle, vomValue);
            Database.Commit(dbName, batchHandle);

            batchHandle = Database.BeginBatch(dbName, null);
            assertTrue(Row.Exists(keyName, batchHandle));
            Collection.DeleteRange(collectionName, batchHandle, new byte[]{}, new byte[]{});
            Database.Commit(dbName, batchHandle);

            batchHandle = Database.BeginBatch(dbName, null);
            assertFalse(Row.Exists(keyName, batchHandle));
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }

    @Test
    public void scan() throws Exception {
        Id dbId = new Id("idp:a:angrybirds", "scan_collection");
        String dbName = dbId.encode();
        Id collectionId = new Id("...", "collection");
        String collectionName = Util.NamingJoin(Arrays.asList(dbName, collectionId.encode()));
        final String keyName = Util.NamingJoin(Arrays.asList(collectionName, "key"));
        // Reference: release/go/src/v.io/v23/vom/testdata/data81/vomdata.vdl
        final byte[] vomValue = {(byte)0x81, 0x06, 0x03, 'a', 'b', 'c'};
        try {
            Database.Create(dbName, anyDbPermissions());
            String batchHandle = Database.BeginBatch(dbId.encode(), null);
            Collection.Create(collectionName, batchHandle, anyCollectionPermissions());
            Row.Put(keyName, batchHandle, vomValue);
            Database.Commit(dbName, batchHandle);

            batchHandle = Database.BeginBatch(dbName, null);
            assertTrue(Row.Exists(keyName, batchHandle));
            final SettableFuture<Void> done = SettableFuture.create();
            Collection.Scan(collectionName, batchHandle, new byte[]{}, new byte[]{},
                    new Collection.ScanCallbacks() {
                @Override
                public void onKeyValue(KeyValue keyValue) {
                    assertEquals("key", keyValue.key);
                    assertArrayEquals(vomValue, keyValue.value);
                }

                @Override
                public void onDone(VError vError) {
                    assertEquals(null, vError);
                    done.set(null);
                }
            });
            done.get(1, TimeUnit.SECONDS);
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }
}
