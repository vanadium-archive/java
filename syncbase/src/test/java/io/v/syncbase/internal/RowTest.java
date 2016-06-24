// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;

import io.v.syncbase.core.Id;
import io.v.syncbase.core.VError;

import static io.v.syncbase.core.TestConstants.anyCollectionPermissions;
import static io.v.syncbase.core.TestConstants.anyDbPermissions;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RowTest {
    @ClassRule
    public static final TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void setUp() throws Exception {
        System.loadLibrary("syncbase");
        Service.Init(folder.newFolder().getAbsolutePath(), true);
        Service.Login("", "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Service.Shutdown();
    }

    @Test
    public void allTest() {
        Id dbId = new Id("idp:a:angrybirds", "collection");
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
            byte[] r = Row.Get(keyName, batchHandle);
            assertArrayEquals(vomValue, r);
            assertTrue(Row.Exists(keyName, batchHandle));
            Row.Delete(keyName, batchHandle);
            assertFalse(Row.Exists(keyName, batchHandle));
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }
}
