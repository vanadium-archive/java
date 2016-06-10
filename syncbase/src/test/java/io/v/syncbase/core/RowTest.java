// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

import org.junit.Before;
import org.junit.Test;

import static io.v.syncbase.core.TestConstants.anyCollectionPermissions;
import static io.v.syncbase.core.TestConstants.anyDbPermissions;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RowTest {
    @Before
    public void setUp() {
        System.loadLibrary("syncbase");
    }

    @Test
    public void all() {
        Id dbId = new Id("idp:a:angrybirds", "core_collection");
        Id collectionId = new Id("...", "collection");
        String key = "key";
        // Reference: release/go/src/v.io/v23/vom/testdata/data81/vomdata.vdl
        byte[] vomValue = {(byte)0x81, 0x06, 0x03, 'a', 'b', 'c'};
        try {
            Database db = Service.database(dbId);
            db.create(anyDbPermissions());
            Collection collection = db.collection(collectionId);
            collection.create(anyCollectionPermissions());
            Row row = collection.row(key);
            row.put(vomValue);
            assertArrayEquals(vomValue, row.get());
            assertTrue(row.exists());
            row.delete();
            assertFalse(row.exists());
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }
}
