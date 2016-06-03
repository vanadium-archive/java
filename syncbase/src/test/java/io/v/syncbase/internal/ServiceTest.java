// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
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
        assertTrue(Service.ListDatabases().isEmpty());
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

    @Test
    public void login() {
        boolean exceptionThrown = false;
        try {
            Service.Login("dummy-provider", "");
        } catch (VError vError) {
            assertEquals("v.io/v23/verror.Unknown", vError.id);
            assertNotNull(vError.message);
            assertTrue(vError.message.contains("dummy-provider"));
            assertNotNull(vError.stack);
            assertEquals(0, vError.actionCode);
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
    }
}
