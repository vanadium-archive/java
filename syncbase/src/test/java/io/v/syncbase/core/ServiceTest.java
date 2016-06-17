// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ServiceTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        System.loadLibrary("syncbase");
        io.v.syncbase.internal.Service.Init(folder.newFolder().getAbsolutePath());
    }

    @After
    public void tearDown() throws Exception {
        io.v.syncbase.internal.Service.Shutdown();
    }

    @Test
    public void listDatabases() {
        assertTrue(Service.listDatabases().isEmpty());
        // TODO(razvanm): Add proper testing after listing starts working.
    }

    @Test
    public void permissions() {
        VersionedPermissions versionedPermissions1 = Service.getPermissions();
        assertNotNull(versionedPermissions1);
        assertTrue(versionedPermissions1.version.length() > 0);
        String json = new String(versionedPermissions1.permissions.json);
        assertTrue(json.contains("Admin"));

        try {
            Service.setPermissions(versionedPermissions1);
            VersionedPermissions versionedPermissions2 = Service.getPermissions();
            assertNotEquals(versionedPermissions1.version, versionedPermissions2.version);
            assertEquals(json, new String(versionedPermissions2.permissions.json));
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }
}
