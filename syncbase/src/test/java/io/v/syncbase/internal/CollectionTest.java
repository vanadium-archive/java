// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CollectionTest {
    @Before
    public void setUp() {
        System.loadLibrary("syncbase");
    }

    @Test
    public void createCollection() {
        Id dbId = new Id("idp:a:angrybirds", "create_collection");
        Id collectionId1 = new Id("idp:u:alice", "collection1");
        Id collectionId2 = new Id("idp:u:alice", "collection2");
        try {
            Database.Create(dbId.encode(), null);
            String batchHandle = Database.BeginBatch(dbId.encode(), null);

            String name1 = Util.NamingJoin(Arrays.asList(dbId.encode(), collectionId1.encode()));
            Collection.Create(name1, batchHandle, null);

            String name2 = Util.NamingJoin(Arrays.asList(dbId.encode(), collectionId2.encode()));
            Collection.Create(name2, batchHandle, null);

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
        Id collectionId = new Id("idp:u:alice", "collection");
        String collectionName = Util.NamingJoin(Arrays.asList(dbName, collectionId.encode()));
        try {
            Database.Create(dbName, null);
            String batchHandle = Database.BeginBatch(dbId.encode(), null);
            Collection.Create(collectionName, batchHandle, null);
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
        Id collectionId1 = new Id("idp:u:alice", "collection1");
        String collectionName1 = Util.NamingJoin(Arrays.asList(dbName, collectionId1.encode()));
        Id collectionId2 = new Id("idp:u:alice", "collection2");
        String collectionName2 = Util.NamingJoin(Arrays.asList(dbName, collectionId2.encode()));
        try {
            Database.Create(dbName, null);
            String batchHandle = Database.BeginBatch(dbId.encode(), null);
            Collection.Create(collectionName1, batchHandle, null);
            // We have not committed the batch yet so Exists should fail.
            assertFalse(Collection.Exists(collectionName1, batchHandle));
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
    public void deleteRangeCollection() {
        Id dbId = new Id("idp:a:angrybirds", "delete_range_collection");
        String dbName = dbId.encode();
        Id collectionId = new Id("idp:u:alice", "collection");
        String collectionName = Util.NamingJoin(Arrays.asList(dbName, collectionId.encode()));
        String keyName = Util.NamingJoin(Arrays.asList(collectionName, "key"));
        // Reference: release/go/src/v.io/v23/vom/testdata/data81/vomdata.vdl
        byte[] vomValue = {(byte)0x81, 0x06, 0x03, 'a', 'b', 'c'};
        try {
            Database.Create(dbName, null);
            String batchHandle = Database.BeginBatch(dbId.encode(), null);
            Collection.Create(collectionName, batchHandle, null);
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
}
