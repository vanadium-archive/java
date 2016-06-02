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
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.access.AccessList;
import io.v.v23.security.access.Constants;
import io.v.v23.security.access.Permissions;
import io.v.v23.security.access.Tag;
import io.v.v23.services.syncbase.Id;
import junit.framework.TestCase;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static class FilterTagTestCase {
        private Permissions input;
        private Iterable<Tag> allowed;
        private Permissions wanted;

        private FilterTagTestCase(Permissions input, Iterable<Tag> allowed, Permissions wanted) {
            this.input = input;
            this.allowed = allowed;
            this.wanted = wanted;
        }
    }

    public void testFilterTags() {
        List<FilterTagTestCase> filterTagTestCases = new ArrayList<>();
        List<BlessingPattern> aclIn = new ArrayList<>();
        aclIn.add(new BlessingPattern("alice"));
        aclIn.add(new BlessingPattern("bob"));
        aclIn.add(new BlessingPattern("carol"));
        List<String> aclNotIn = new ArrayList<>();
        aclNotIn.add("alice:enemy");
        AccessList acl = new AccessList(aclIn, aclNotIn);

        Map<String, AccessList> mapping = new HashMap<>();
        mapping.put(Constants.DEBUG.getValue(), acl);
        mapping.put(Constants.RESOLVE.getValue(), acl);
        mapping.put(Constants.READ.getValue(), acl);
        mapping.put(Constants.ADMIN.getValue(), acl);

        Permissions canonicalPerms = new Permissions(mapping);

        Map<String, AccessList> noDebugMapping = new HashMap<>(mapping);
        noDebugMapping.remove(Constants.DEBUG.getValue());
        Permissions noDebugPerms = new Permissions(noDebugMapping);
        Set<Tag> noDebugTags = new HashSet<>();
        Collections.addAll(noDebugTags, Constants.RESOLVE, Constants.READ, Constants.WRITE,
                Constants.ADMIN);

        Map<String, AccessList> noResolveReadMapping = new HashMap<>(mapping);
        noResolveReadMapping.remove(Constants.RESOLVE.getValue());
        noResolveReadMapping.remove(Constants.READ.getValue());
        Permissions noResolveReadPerms = new Permissions(noResolveReadMapping);
        List<Tag> noResolveReadTags = new ArrayList<>();
        Collections.addAll(noResolveReadTags, Constants.DEBUG, Constants.WRITE, Constants.ADMIN);

        List<Tag> allTags = new ArrayList<>();
        Collections.addAll(allTags, Constants.DEBUG, Constants.RESOLVE, Constants.READ,
                Constants.WRITE, Constants.ADMIN);

        filterTagTestCases.add(new FilterTagTestCase(new Permissions(), new ArrayList<Tag>(),
                new Permissions()));
        filterTagTestCases.add(new FilterTagTestCase(canonicalPerms, new HashSet<Tag>(),
                new Permissions()));
        filterTagTestCases.add(new FilterTagTestCase(canonicalPerms, noDebugTags, noDebugPerms));
        filterTagTestCases.add(new FilterTagTestCase(canonicalPerms, noResolveReadTags,
                noResolveReadPerms));
        filterTagTestCases.add(new FilterTagTestCase(canonicalPerms, allTags, canonicalPerms));

        // Confirm that things match up correctly.
        for (FilterTagTestCase test : filterTagTestCases) {
            Permissions actualPerms = Util.filterPermissionsByTags(test.input, test.allowed);
            assertEquals(actualPerms, test.wanted);

            // Confirm the filtered version is independent from the original.
            Permissions origPerms = test.input;
            String adminStr = Constants.ADMIN.getValue();
            if (actualPerms.get(adminStr) != null) {
                List<BlessingPattern> actualIn = actualPerms.get(adminStr).getIn();
                List<BlessingPattern> origIn = origPerms.get(adminStr).getIn();

                // It's possible that the admin access list is the same reference instead of copied.
                // Confirm equality. Change the admin access list. Then confirm inequality.
                assertEquals(actualIn, origIn);
                actualIn.clear();
                assertFalse(actualIn.equals(origIn));
            }

        }
    }
}
