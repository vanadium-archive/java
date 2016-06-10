// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ServiceTest {
    @Before
    public void setUp() {
        System.loadLibrary("syncbase");
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
