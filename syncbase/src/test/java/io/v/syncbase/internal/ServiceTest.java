// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import io.v.syncbase.core.VError;
import io.v.syncbase.core.VersionedPermissions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ServiceTest {
    @ClassRule
    public static final TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void setUp() throws Exception {
        System.loadLibrary("syncbase");
        Service.Init(folder.newFolder().getAbsolutePath(), true, 0);
        Service.Login("", "");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        Service.Shutdown();
    }

    @Test
    public void listDatabases() {
        assertTrue(Service.ListDatabases().isEmpty());
        // TODO(razvanm): Add proper testing after listing starts working.
    }

    @Test
    public void getPermissions() {
        VersionedPermissions versionedPermissions1 = Service.GetPermissions();
        assertNotNull(versionedPermissions1);
        assertTrue(versionedPermissions1.version.length() > 0);
        String json = new String(versionedPermissions1.permissions.json);
        assertTrue(json.contains("Admin"));

        try {
            Service.SetPermissions(versionedPermissions1);
            VersionedPermissions versionedPermissions2 = Service.GetPermissions();
            assertEquals("1", versionedPermissions2.version);
            assertEquals(json, new String(versionedPermissions2.permissions.json));
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }
}
