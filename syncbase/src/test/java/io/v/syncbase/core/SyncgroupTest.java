// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.core;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.v.syncbase.core.TestConstants.anyCollectionPermissions;
import static io.v.syncbase.core.TestConstants.anyDbPermissions;
import static io.v.syncbase.core.TestConstants.anySyncgroupPermissions;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SyncgroupTest {
    @ClassRule
    public static TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void setUp() throws Exception {
        System.loadLibrary("syncbase");
        io.v.syncbase.internal.Service.Init(folder.newFolder().getAbsolutePath());
        io.v.syncbase.internal.Service.Serve();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        io.v.syncbase.internal.Service.Shutdown();
    }

    @Test
    public void create() {
        Id dbId = new Id("idp:a:angrybirds", "core_create_syncgroups");
        Id sgId = new Id("...", "syncgroup");
        Id collectionId = new Id("...", "collection");
        try {
            Database db = Service.database(dbId);
            db.create(anyDbPermissions());
            db.collection(collectionId).create(anyCollectionPermissions());
            SyncgroupSpec spec = new SyncgroupSpec();
            spec.collections = Arrays.asList(collectionId);
            spec.permissions = anySyncgroupPermissions();
            SyncgroupMemberInfo info = new SyncgroupMemberInfo();
            // TODO(razvanm): Pick some meaningful values.
            info.syncPriority = 1;
            info.blobDevType = 2;
            Syncgroup syncgroup = db.syncgroup(sgId);
            syncgroup.create(spec, info);

            List<Id> syncgroups = db.listSyncgroups();
            assertEquals(1, syncgroups.size());
            Id actual = syncgroups.get(0);
            assertEquals(sgId.blessing, actual.blessing);
            assertEquals(sgId.name, actual.name);

            VersionedSyncgroupSpec verSpec = syncgroup.getSpec();
            assertNotNull(verSpec.version);
            assertTrue(verSpec.version.length() > 0);
            assertNotNull(verSpec.syncgroupSpec);
            assertEquals(1, verSpec.syncgroupSpec.collections.size());
            // The trim is used to remove a new line.
            assertEquals(
                    new String(spec.permissions.json),
                    new String(verSpec.syncgroupSpec.permissions.json).trim());
            actual = syncgroups.get(0);
            assertEquals(sgId.blessing, actual.blessing);
            assertEquals(sgId.name, actual.name);

            verSpec.syncgroupSpec.description = "Dummy description";
            syncgroup.setSpec(verSpec);
            assertEquals(
                    verSpec.syncgroupSpec.description,
                    syncgroup.getSpec().syncgroupSpec.description);

            Map<String, SyncgroupMemberInfo> members = syncgroup.getMembers();
            assertNotNull(members);
            assertEquals(1, members.size());
            assertTrue(members.keySet().iterator().next().length() > 0);
            assertEquals(info.syncPriority, members.values().iterator().next().syncPriority);
            assertEquals(info.blobDevType, members.values().iterator().next().blobDevType);
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }

    @Test
    public void destroy() {
        Id dbId = new Id("idp:a:angrybirds", "destroy_syncgroup");
        Id sgId = new Id("idp:u:alice", "syncgroup");
        // TODO(razvanm): We'll have to update this after the destroy lands.
        boolean exceptionThrown = false;
        try {
            Database db = Service.database(dbId);
            db.create(anyDbPermissions());
            db.syncgroup(sgId).destroy();
        } catch (VError vError) {
            assertEquals("v.io/v23/verror.NotImplemented", vError.id);
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
    }

    @Test
    public void join() {
        Id dbId = new Id("idp:a:angrybirds", "core_join_syncgroup");
        Id sgId = new Id("idp:u:alice", "syncgroup");
        boolean exceptionThrown = false;
        try {
            Database db = Service.database(dbId);
            db.syncgroup(sgId).join("", new ArrayList<String>(), new SyncgroupMemberInfo());
        } catch (VError vError) {
            assertEquals("v.io/v23/verror.NoExist", vError.id);
            assertNotNull(vError.message);
            assertNotNull(vError.stack);
            assertEquals(0, vError.actionCode);
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
    }

    @Test
    public void leave() {
        Id dbId = new Id("idp:a:angrybirds", "core_leave_syncgroups");
        String dbName = dbId.encode();
        Id sgId = new Id("idp:u:alice", "syncgroup");
        boolean exceptionThrown = false;
        try {
            Database db = Service.database(dbId);
            db.create(anyDbPermissions());
            db.syncgroup(sgId).leave();
        }  catch (VError vError) {
            assertEquals("v.io/v23/verror.NotImplemented", vError.id);
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
    }

    @Test
    public void eject() {
        Id dbId = new Id("idp:a:angrybirds", "core_eject_from_syncgroup");
        String dbName = dbId.encode();
        Id sgId = new Id("idp:u:alice", "syncgroup");
        boolean exceptionThrown = false;
        try {
            Database db = Service.database(dbId);
            db.create(anyDbPermissions());
            db.syncgroup(sgId).eject("");
        }  catch (VError vError) {
            assertEquals("v.io/v23/verror.NotImplemented", vError.id);
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
    }
}
