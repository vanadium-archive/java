// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
//

package io.v.impl.google.namespace;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import io.v.impl.google.services.mounttable.MountTableServer;
import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.namespace.Namespace;
import io.v.v23.naming.Endpoint;
import io.v.v23.naming.GlobReply;
import io.v.v23.naming.MountEntry;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerCall;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.VSecurity;
import io.v.v23.security.access.AccessList;
import io.v.v23.security.access.Constants;
import io.v.v23.security.access.Permissions;
import io.v.v23.services.permissions.ObjectServer;
import io.v.v23.verror.VException;
import junit.framework.TestCase;
import org.joda.time.Duration;

import static com.google.common.truth.Truth.assertThat;

/**
 * Test the {@link NamespaceImpl} implementation.
 */
public class NamespaceTest extends TestCase {
    private VContext ctx;
    private VContext dummyServerCtx;
    private Endpoint mountTableEndpoint;
    private Endpoint dummyServerEndpoint;

    @Override
    protected void setUp() throws Exception {
        ctx = V.init();
        ctx = V.withListenSpec(ctx, V.getListenSpec(ctx).withAddress(
                new ListenSpec.Address("tcp", "localhost:0")));
        dummyServerCtx = V.withNewServer(ctx, "", new DummyServer(),
                VSecurity.newAllowEveryoneAuthorizer());
        Server dummyServer = V.getServer(dummyServerCtx);
        assertThat(dummyServer).isNotNull();
        assertThat(dummyServer.getStatus()).isNotNull();
        assertThat(dummyServer.getStatus().getEndpoints()).isNotEmpty();
        dummyServerEndpoint = dummyServer.getStatus().getEndpoints()[0];
        AccessList acl = new AccessList(
                ImmutableList.of(new BlessingPattern("...")), ImmutableList.<String>of());
        Permissions allowAll = new Permissions(ImmutableMap.of(
                Constants.READ.getValue(), acl,
                Constants.WRITE.getValue(), acl,
                Constants.ADMIN.getValue(), acl));
        ctx = MountTableServer.withNewServer(ctx, new MountTableServer.Params()
                .withPermissions(ImmutableMap.of("test", allowAll))
                .withStatsPrefix("test"));
        Server mtServer = V.getServer(ctx);
        assertThat(mtServer).isNotNull();
        assertThat(mtServer.getStatus()).isNotNull();
        assertThat(mtServer.getStatus().getEndpoints()).isNotEmpty();
        mountTableEndpoint = mtServer.getStatus().getEndpoints()[0];
        Namespace n = V.getNamespace(ctx);
        n.setRoots(ImmutableList.of(mountTableEndpoint.name()));
    }

    @Override
    protected void tearDown() throws Exception {
        Server mtServer = V.getServer(ctx);
        if (mtServer != null) {
            mtServer.stop();
        }
        Server dummyServer = V.getServer(dummyServerCtx);
        if (dummyServer != null) {
            dummyServer.stop();
        }
    }

    private Iterable<String> globNames(Iterable<GlobReply> globReplies) {
        return Iterables.transform(globReplies, new Function<GlobReply, String>() {
            @Override
            public String apply(GlobReply reply) {
                if (reply instanceof GlobReply.Entry) {
                    return ((GlobReply.Entry) reply).getElem().getName();
                } else {
                    return ((GlobReply.Error) reply).getElem().getError().getMessage();
                }
            }
        });
    }

    public void testMount() throws Exception {
        Namespace n = V.getNamespace(ctx);
        n.mount(ctx, "test/test", dummyServerEndpoint.name(), Duration.standardDays(1));
        assertThat(globNames(n.glob(ctx, "test/*"))).containsExactly("test/test");
        n.unmount(ctx, "test/test", "");
        assertThat(globNames(n.glob(ctx, "test/*"))).isEmpty();
    }

    public void testDelete() throws Exception {
        Namespace n = V.getNamespace(ctx);
        n.mount(ctx, "test/test/test", dummyServerEndpoint.name(), Duration.standardDays(1));
        n.mount(ctx, "test/test/test2", dummyServerEndpoint.name(), Duration.standardDays(1));
        assertThat(globNames(n.glob(ctx, "test/*/*"))).containsExactly(
                "test/test/test", "test/test/test2");
        // Shouldn't delete anything since the dir contains children.
        try {
            n.delete(ctx, "test/test", false);
            fail("Namespace.delete() should have failed because a directory has children");
        } catch (VException e) {
            // OK
        }
        assertThat(globNames(n.glob(ctx, "test/*/*"))).containsExactly(
                "test/test/test", "test/test/test2");
        n.delete(ctx, "test/test", true);
        assertThat(globNames(n.glob(ctx, "test/*"))).isEmpty();
    }

    public void testResolve() throws Exception {
        Namespace n = V.getNamespace(ctx);
        n.mount(ctx, "test/test", dummyServerEndpoint.name(), Duration.standardDays(1));
        MountEntry entry = n.resolve(ctx, "test/test");
        assertThat(entry).isNotNull();
        assertThat(entry.getServers()).isNotNull();
        assertThat(entry.getServers()).hasSize(1);
        assertThat(entry.getServers().get(0).getServer()).isEqualTo(dummyServerEndpoint.name());

    }

    public void testResolveToMountTable() throws Exception {
        Namespace n = V.getNamespace(ctx);
        n.mount(ctx, "test/test", dummyServerEndpoint.name(), Duration.standardDays(1));
        MountEntry entry = n.resolveToMountTable(ctx, "test/test");
        assertThat(entry).isNotNull();
        assertThat(entry.getServers()).isNotNull();
        assertThat(entry.getServers()).hasSize(1);
        assertThat(entry.getServers().get(0).getServer()).isEqualTo(mountTableEndpoint.name());
    }

    private static class DummyServer implements ObjectServer {
        @Override
        public void setPermissions(VContext ctx, ServerCall call, Permissions permissions,
                                   String version) throws VException {
            throw new VException("Unimplemented!");
        }
        @Override
        public GetPermissionsOut getPermissions(VContext ctx, ServerCall call) throws VException {
            throw new VException("Unimplemented!");
        }
    }
}
