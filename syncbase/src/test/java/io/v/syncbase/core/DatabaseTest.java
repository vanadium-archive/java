// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.List;

import static io.v.syncbase.core.TestConstants.anyDbPermissions;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DatabaseTest {
    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

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
    public void create() {
        Id dbId = new Id("idp:a:angrybirds", "core_create_db");
        // The instance is empty so creating of a database should succeed.
        try {
            Service.database(dbId).create(anyDbPermissions());
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }

        // Creating the same database should raise an exception.
        boolean exceptionThrown = false;
        try {
            Service.database(dbId).create(anyDbPermissions());
        } catch (VError vError) {
            assertEquals("v.io/v23/verror.Exist", vError.id);
            assertNotNull(vError.message);
            assertNotNull(vError.stack);
            assertEquals(0, vError.actionCode);
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
    }

    @Test
    public void destroy() {
        Id dbId = new Id("idp:a:angrybirds", "core_destroy_db");
        try {
            Database db = Service.database(dbId);
            db.create(anyDbPermissions());
            db.destroy();
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }

    @Test
    public void exists() {
        Id dbId = new Id("idp:a:angrybirds", "core_exists_db");
        try {
            Database db = Service.database(dbId);
            // We have not created the database yet so Exists should fail.
            assertFalse(db.exists());
            // The instance is empty so creating of a database should succeed.
            db.create(anyDbPermissions());
            // Exists should succeed now.
            assertTrue(db.exists());
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }

    @Test
    public void permissions() {
        Id dbId = new Id("idp:a:angrybirds", "core_permissions_db");
        try {
            Database db = Service.database(dbId);
            db.create(anyDbPermissions());
            VersionedPermissions versionedPermissions1 = db.getPermissions();
            assertNotNull(versionedPermissions1);
            assertTrue(versionedPermissions1.version.length() > 0);
            String json = new String(versionedPermissions1.permissions.json);
            assertTrue(json.contains("Admin"));

            db.setPermissions(versionedPermissions1);
            VersionedPermissions versionedPermissions2 = db.getPermissions();
            assertNotEquals(versionedPermissions1.version, versionedPermissions2.version);
            assertEquals(json, new String(versionedPermissions2.permissions.json));
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }

    @Test
    public void listSyncgroups() {
        Id dbId = new Id("idp:a:angrybirds", "core_list_syncgroups");
        try {
            Database db = Service.database(dbId);
            db.create(anyDbPermissions());
            List<Id> syncgroups = db.listSyncgroups();
            assertNotNull(syncgroups);
            assertEquals(0, syncgroups.size());
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }
}
