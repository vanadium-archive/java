// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase;

import org.junit.Test;

public class SyncbaseTest {
    @Test
    public void createDatabase() {
        Syncbase.DatabaseOptions opts = new Syncbase.DatabaseOptions();
        opts.rootDir = "/tmp";
        // TODO(sadovsky): Restore this once we figure out what Vanadium context to pass.
        // Syncbase.database(opts);
    }
}