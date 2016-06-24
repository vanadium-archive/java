// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static io.v.syncbase.core.TestConstants.anyCollectionPermissions;
import static io.v.syncbase.core.TestConstants.anyDbPermissions;
import static org.junit.Assert.fail;

public class BatchDatabaseTest {
    @ClassRule
    public static final TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void setUp() throws Exception {
        System.loadLibrary("syncbase");
        io.v.syncbase.internal.Service.Init(folder.newFolder().getAbsolutePath(), true);
        io.v.syncbase.internal.Service.Login("", "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        io.v.syncbase.internal.Service.Shutdown();
    }

    @Test
    public void commitAndAbort() {
        Id dbId = new Id("idp:a:angrybirds", "core_abort_db");
        Id collectionId = new Id("...", "collection");
        try {
            Database db = Service.database(dbId);
            db.create(anyDbPermissions());
            BatchDatabase batchDb = db.beginBatch(null);
            Collection collection = batchDb.collection(collectionId);
            collection.create(anyCollectionPermissions());
            batchDb.abort();
            batchDb = db.beginBatch(null);
            collection = batchDb.collection(collectionId);
            // This should work because we abort the previous batch.
            collection.create(anyCollectionPermissions());
            batchDb.commit();
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }
}
