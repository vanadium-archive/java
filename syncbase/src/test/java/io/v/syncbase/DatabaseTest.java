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
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import io.v.syncbase.exception.SyncbaseException;

import static io.v.syncbase.TestUtil.setUpDatabase;

public class DatabaseTest {
    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        setUpDatabase(folder.newFolder());
    }

    @After
    public void tearDown() {
        Syncbase.shutdown();
    }

    @Test
    public void noCollections() throws IOException, SyncbaseException,
            ExecutionException, InterruptedException {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("No collections");

        // Try to create a syncgroup with no collections
        Syncbase.database().syncgroup("aSyncgroup", new ArrayList<Collection>());
    }

}
