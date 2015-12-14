// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.debug;

import io.v.baku.toolkit.VAndroidTestCase;
import io.v.v23.VFutures;
import io.v.v23.context.VContext;
import io.v.v23.syncbase.SyncbaseApp;
import io.v.v23.verror.ExistException;

import static io.v.v23.VFutures.sync;

public class SyncbaseClientTest extends VAndroidTestCase {
    private static final String APP = SyncbaseClientTest.class.toString();

    public void testAppPersistence() throws Exception {
        final VContext ctx = getVContext();

        try (final SyncbaseClient sb = createSyncbaseClient()) {
            final SyncbaseApp app = first(sb.getRxClient()).getApp(APP);
            try {
                sync(app.create(ctx, null));
            } catch (final ExistException ignore) {
            }
            assertEquals(true, (boolean) sync(app.exists(ctx)));
        }

        try (final SyncbaseClient sb = createSyncbaseClient()) {
            final SyncbaseApp app = first(sb.getRxClient()).getApp(APP);
            assertEquals(true, (boolean) sync(app.exists(ctx)));
            sync(app.destroy(ctx));
            assertEquals(false, (boolean) sync(app.exists(ctx)));
        }
    }
}
