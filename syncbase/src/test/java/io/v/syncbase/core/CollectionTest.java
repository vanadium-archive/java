// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.List;

import static io.v.syncbase.core.TestConstants.anyCollectionPermissions;
import static io.v.syncbase.core.TestConstants.anyDbPermissions;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CollectionTest {
    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void setUp() throws Exception {
        System.loadLibrary("syncbase");
        io.v.syncbase.internal.Service.Init(folder.newFolder().getAbsolutePath());
        io.v.syncbase.internal.Service.Serve();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        io.v.syncbase.internal.Service.Shutdown();
    }

    @Test
    public void create() {
        Id dbId = new Id("idp:a:angrybirds", "core_create_collection");
        Id collectionId1 = new Id("...", "collection1");
        Id collectionId2 = new Id("...", "collection2");
        try {
            Database db = Service.database(dbId);
            db.create(anyDbPermissions());
            BatchDatabase batchDb = db.beginBatch(null);

            batchDb.collection(collectionId1).create(anyCollectionPermissions());
            batchDb.collection(collectionId2).create(anyCollectionPermissions());

            List<Id> collections = batchDb.listCollections();
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
    public void destroy() {
        Id dbId = new Id("idp:a:angrybirds", "core_destroy_collection");
        Id collectionId = new Id("...", "collection");
        try {
            Database db = Service.database(dbId);
            db.create(anyDbPermissions());
            BatchDatabase batchDb = db.beginBatch(null);
            batchDb.collection(collectionId).create(anyCollectionPermissions());
            batchDb.commit();

            batchDb = db.beginBatch(null);
            batchDb.collection(collectionId).destroy();
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }

    @Test
    public void exists() {
        Id dbId = new Id("idp:a:angrybirds", "core_exists_collection");
        Id collectionId1 = new Id("...", "collection1");
        Id collectionId2 = new Id("...", "collection2");
        try {
            Database db = Service.database(dbId);
            db.create(anyDbPermissions());
            BatchDatabase batchDb = db.beginBatch(null);
            Collection collection1 = batchDb.collection(collectionId1);
            collection1.create(anyCollectionPermissions());
            // We have not committed the batch yet so exists() should fail.
            assertFalse(collection1.exists());
            batchDb.commit();
            assertTrue(db.collection(collectionId1).exists());
            assertFalse(db.collection(collectionId2).exists());
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }

    @Test
    public void permissions() {
        Id dbId = new Id("idp:a:angrybirds", "core_permissions_collection");
        Id collectionId = new Id("...", "collection");
        try {
            Database db = Service.database(dbId);
            db.create(anyDbPermissions());
            Collection collection = db.collection(collectionId);
            collection.create(anyCollectionPermissions());
            Permissions permissions = collection.getPermissions();
            assertNotNull(permissions);
            String json = new String(permissions.json);
            assertTrue(json.contains("Admin"));

            collection.setPermissions(permissions);
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }

    @Test
    public void deleteRangeCollection() {
        Id dbId = new Id("idp:a:angrybirds", "core_delete_range_collection");
        Id collectionId = new Id("...", "collection");
        String key = "key";
        // Reference: release/go/src/v.io/v23/vom/testdata/data81/vomdata.vdl
        byte[] vomValue = {(byte)0x81, 0x06, 0x03, 'a', 'b', 'c'};
        try {
            Database db = Service.database(dbId);
            db.create(anyDbPermissions());
            Collection collection = db.collection(collectionId);
            collection.create(anyCollectionPermissions());
            collection.put(key, vomValue);
            assertTrue(collection.row(key).exists());

            collection.deleteRange(new byte[]{}, new byte[]{});
            assertFalse(collection.row(key).exists());
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }
}
