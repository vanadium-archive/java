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
import static org.junit.Assert.assertEquals;

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

    @Test
    public void testWatchChangeHandlerOptionsBuilder() {
        Database.AddWatchChangeHandlerOptions opts;

        // Wildcard and prefix tests.
        opts = new Database.AddWatchChangeHandlerOptions.Builder()
                        .build();
        assertEquals("%", opts.blessing);
        assertEquals("%", opts.name);
        assertEquals("%", opts.row);

        opts = new Database.AddWatchChangeHandlerOptions.Builder()
                        .setCollectionId(new Id("a", "b"))
                        .build();
        assertEquals("a", opts.blessing);
        assertEquals("b", opts.name);
        assertEquals("%", opts.row);

        opts = new Database.AddWatchChangeHandlerOptions.Builder()
                .setCollectionNamePrefix("c")
                .build();
        assertEquals("%", opts.blessing);
        assertEquals("c%", opts.name);
        assertEquals("%", opts.row);

        opts = new Database.AddWatchChangeHandlerOptions.Builder()
                .setRowKey("d")
                .build();
        assertEquals("%", opts.blessing);
        assertEquals("%", opts.name);
        assertEquals("d", opts.row);

        opts = new Database.AddWatchChangeHandlerOptions.Builder()
                .setRowKeyPrefix("e")
                .build();
        assertEquals("%", opts.blessing);
        assertEquals("%", opts.name);
        assertEquals("e%", opts.row);

        // Escaping tests. %, _ and \ are special characters.
        opts = new Database.AddWatchChangeHandlerOptions.Builder()
                .setCollectionId(new Id("%", "_"))
                .build();
        assertEquals("\\%", opts.blessing);
        assertEquals("\\_", opts.name);
        assertEquals("%", opts.row);

        opts = new Database.AddWatchChangeHandlerOptions.Builder()
                .setCollectionNamePrefix("\\")
                .build();
        assertEquals("%", opts.blessing);
        assertEquals("\\\\%", opts.name);
        assertEquals("%", opts.row);

        opts = new Database.AddWatchChangeHandlerOptions.Builder()
                .setRowKey("%%")
                .build();
        assertEquals("%", opts.blessing);
        assertEquals("%", opts.name);
        assertEquals("\\%\\%", opts.row);

        opts = new Database.AddWatchChangeHandlerOptions.Builder()
                .setRowKeyPrefix("_\\_")
                .build();
        assertEquals("%", opts.blessing);
        assertEquals("%", opts.name);
        assertEquals("\\_\\\\\\_%", opts.row);
    }

}
