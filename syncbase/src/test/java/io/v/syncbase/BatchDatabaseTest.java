// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import io.v.syncbase.DatabaseHandle.CollectionOptions;
import io.v.syncbase.exception.SyncbaseException;

import static io.v.syncbase.TestUtil.setUpDatabase;

public class BatchDatabaseTest {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws IOException, InterruptedException, ExecutionException,
            SyncbaseException {
        setUpDatabase(folder.newFolder());
    }

    @After
    public void tearDown() {
        Syncbase.shutdown();
    }

    @Test
    public void attemptCreateSyncgroupWithinBatch() throws SyncbaseException,
            ExecutionException, InterruptedException {
        BatchDatabase batch = Syncbase.database().beginBatch(new Database.BatchOptions());

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Cannot create syncgroup in a batch");

        batch.collection("aCollection", new CollectionOptions());
    }

}
