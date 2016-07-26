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
import java.util.concurrent.Executor;

import io.v.syncbase.DatabaseHandle.CollectionOptions;
import io.v.syncbase.exception.SyncbaseException;

import static io.v.syncbase.TestUtil.setUpDatabase;

public class CollectionTest {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final Executor sameThreadExecutor = new Executor() {
        public void execute(Runnable runnable) {
            runnable.run();
        }
    };

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
    public void badName() throws SyncbaseException,
            ExecutionException, InterruptedException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("invalid name");

        // Create with invalid name
        Syncbase.database().createNamedCollection("control\0char", new CollectionOptions());
    }

    @Test
    public void attemptGetSyncgroupWithinBatch() throws SyncbaseException,
            ExecutionException, InterruptedException {
        BatchDatabase batch = Syncbase.database().beginBatch(new Database.BatchOptions());

        // Create collection without a syncgroup
        Collection collection = batch.createCollection(
                new CollectionOptions().setWithoutSyncgroup(true));

        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Must not call getSyncgroup within batch");
        collection.getSyncgroup();
    }
}
