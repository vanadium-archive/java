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

public class DatabaseTest {
    @Before
    public void setUp() {
        System.loadLibrary("syncbase");
    }

    @Test
    public void createDatabase() {
        Id dbId = new Id("blessing", "db");
        // The instance is empty so creating of a database should succeed.
        try {
            Database.Create(dbId.toString(), null);
        } catch (VError vError) {
            fail();
        }

        // Creating the same database should raise an exception.
        boolean exceptionThrown = false;
        try {
            Database.Create(dbId.toString(), null);
        } catch (VError vError) {
            assertEquals("v.io/v23/verror.Exist", vError.id);
            assertNotNull(vError.message);
            assertNotNull(vError.stack);
            assertEquals(0, vError.actionCode);
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
    }
}
