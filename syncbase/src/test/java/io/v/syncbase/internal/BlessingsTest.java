// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import io.v.syncbase.core.VError;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BlessingsTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        System.loadLibrary("syncbase");
        Service.Init(folder.newFolder().getAbsolutePath());
    }

    @After
    public void tearDown() throws Exception {
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
