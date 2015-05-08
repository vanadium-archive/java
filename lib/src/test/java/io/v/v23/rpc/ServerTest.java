// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;

import junit.framework.TestCase;

import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.verror.VException;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

public class ServerTest extends TestCase {
    private final Dispatcher dummyDispatcher;

    public ServerTest() {
        dummyDispatcher = new Dispatcher() {
            @Override
            public ServiceObjectWithAuthorizer lookup(String suffix) throws VException {
                throw new VException("Lookup unimplemented.");
            }
        };
    }

    public void testAddRemoveName() throws Exception {
        final VContext ctx = V.init();
        final Server s = V.newServer(ctx);
        s.listen(V.getListenSpec(ctx));
        s.serve("name1", dummyDispatcher);
        s.addName("name2");
        assertThat(getNames(s)).containsExactly("name1", "name2");
        s.addName("name2");
        assertThat(getNames(s)).containsExactly("name1", "name2");
        s.addName("name3");
        assertThat(getNames(s)).containsExactly("name1", "name2", "name3");
        s.removeName("name2");
        assertThat(getNames(s)).containsExactly("name1", "name3");
        s.removeName("name2");
        assertThat(getNames(s)).containsExactly("name1", "name3");
    }

    private static List<String> getNames(Server s) {
        List<String> names = new ArrayList<String>();
        for (MountStatus mount : s.getStatus().getMounts()) {
            names.add(mount.getName());
        }
        names = ImmutableSet.copyOf(names).asList();
        return Ordering.natural().sortedCopy(names);
    }
}
