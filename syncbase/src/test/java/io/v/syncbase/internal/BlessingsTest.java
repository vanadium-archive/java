// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.internal;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BlessingsTest {
    @Before
    public void setUp() {
        System.loadLibrary("syncbase");
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
