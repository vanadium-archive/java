// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.util;

import io.v.impl.google.namespace.NamespaceTestUtil;
import io.v.v23.V;
import io.v.v23.V23TestUtil;
import io.v.v23.context.VContext;
import io.v.v23.namespace.Namespace;
import io.v.v23.naming.Endpoint;
import io.v.v23.services.syncbase.Id;
import junit.framework.TestCase;
import org.joda.time.Duration;

import static com.google.common.truth.Truth.assertThat;
import static io.v.v23.VFutures.sync;

/**
 * Tests for various utility functions in {@link Util}.
 */
public class UtilTest extends TestCase {
    private VContext ctx;
    private VContext dummyServerCtx;
    private Endpoint dummyServerEndpoint;

    @Override
    protected void setUp() throws Exception {
        ctx = V.init();
        ctx = V.init();
        dummyServerCtx = V23TestUtil.withDummyServer(ctx);
        dummyServerEndpoint = V23TestUtil.getServerEndpoint(dummyServerCtx);
        ctx = NamespaceTestUtil.withTestMountServer(ctx);
    }

    public void testListChildren() throws Exception {
        Namespace n = V.getNamespace(ctx);
        sync(n.mount(ctx, "appblessing,database/userblessing,collection1", dummyServerEndpoint.name(), Duration.standardDays(1)));
        sync(n.mount(ctx, "appblessing,database/userblessing,collection2", dummyServerEndpoint.name(), Duration.standardDays(1)));
        assertThat(sync(Util.listChildIds(ctx, "appblessing,database"))).containsExactly(new Id("userblessing", "collection1"), new Id("userblessing", "collection2"));
    }
}
