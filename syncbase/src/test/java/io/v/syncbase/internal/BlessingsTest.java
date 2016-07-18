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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BlessingsTest {
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
    public void DebugStringTest() {
        String s = Blessings.DebugString();
        assertTrue(s.contains("Default Blessings"));
    }

    @Test
    public void AppBlessingFromContext() {
        try {
            String s = Blessings.AppBlessingFromContext();
            assertNotNull(s);
            assertFalse(s.isEmpty());
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }

    @Test
    public void UserBlessingFromContext() {
        try {
            String s = Blessings.UserBlessingFromContext();
            assertNotNull(s);
            assertFalse(s.isEmpty());
        } catch (VError vError) {
            vError.printStackTrace();
            fail(vError.toString());
        }
    }
}
