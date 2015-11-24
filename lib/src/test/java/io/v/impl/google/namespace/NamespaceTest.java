// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
//

package io.v.impl.google.namespace;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.Uninterruptibles;

import io.v.v23.VIterable;
import junit.framework.TestCase;

import org.joda.time.Duration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.v.impl.google.services.mounttable.MountTableServer;
import io.v.v23.V;
import io.v.v23.context.CancelableVContext;
import io.v.v23.context.VContext;
import io.v.v23.namespace.Namespace;
import io.v.v23.naming.Endpoint;
import io.v.v23.naming.GlobReply;
import io.v.v23.naming.MountEntry;
import io.v.v23.rpc.Callback;
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

import static com.google.common.truth.Truth.assertThat;
import static io.v.v23.VFutures.sync;

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

    public void testMountAndUnmount() throws Exception {
        Namespace n = V.getNamespace(ctx);
        sync(n.mount(ctx, "test/test", dummyServerEndpoint.name(), Duration.standardDays(1)));
        assertThat(globNames(sync(n.glob(ctx, "test/*")))).containsExactly("test/test");
        sync(n.unmount(ctx, "test/test", ""));
        assertThat(globNames(sync(n.glob(ctx, "test/*")))).isEmpty();
    }

    public void testDelete() throws Exception {
        Namespace n = V.getNamespace(ctx);
        sync(n.mount(ctx, "test/test/test", dummyServerEndpoint.name(), Duration.standardDays(1)));
        sync(n.mount(ctx, "test/test/test2", dummyServerEndpoint.name(), Duration.standardDays(1)));
        assertThat(globNames(sync(n.glob(ctx, "test/*/*")))).containsExactly(
                "test/test/test", "test/test/test2");
        // Shouldn't delete anything since the dir contains children.
        try {
            sync(n.delete(ctx, "test/test", false));
            fail("Namespace.delete() should have failed because a directory has children");
        } catch (VException e) {
            // OK
        }
        assertThat(globNames(sync(n.glob(ctx, "test/*/*")))).containsExactly(
                "test/test/test", "test/test/test2");
        sync(n.delete(ctx, "test/test", true));
        assertThat(globNames(sync(n.glob(ctx, "test/*")))).isEmpty();
    }

    public void testGlob() throws Exception {
        Namespace n = V.getNamespace(ctx);
        sync(n.mount(ctx, "test/test", dummyServerEndpoint.name(), Duration.standardDays(1)));
        List<GlobReply> result = Lists.newArrayList(sync(n.glob(ctx, "test/*")));
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getElem()).isInstanceOf(MountEntry.class);
        assertThat(((MountEntry) (result.get(0).getElem())).getName()).isEqualTo("test/test");
    }

    public void testGlobWithContextCancel() throws Exception {
        Namespace n = V.getNamespace(ctx);
        sync(n.mount(ctx, "test/test", dummyServerEndpoint.name(), Duration.standardDays(1)));

        CancelableVContext cancelContext = ctx.withCancel();
        ListenableFuture<VIterable<GlobReply>> result = n.glob(cancelContext, "test/*");
        cancelContext.cancel();
        List<GlobReply> replies = Lists.newArrayList(result.get());
        assertThat(replies).isEmpty();
    }

    public void testResolve() throws Exception {
        Namespace n = V.getNamespace(ctx);
        sync(n.mount(ctx, "test/test", dummyServerEndpoint.name(), Duration.standardDays(1)));
        MountEntry entry = sync(n.resolve(ctx, "test/test"));
        assertThat(entry).isNotNull();
        assertThat(entry.getServers()).isNotNull();
        assertThat(entry.getServers()).hasSize(1);
        assertThat(entry.getServers().get(0).getServer()).isEqualTo(dummyServerEndpoint.name());
    }

    public void testResolveToMountTable() throws Exception {
        Namespace n = V.getNamespace(ctx);
        sync(n.mount(ctx, "test/test", dummyServerEndpoint.name(), Duration.standardDays(1)));
        MountEntry entry = sync(n.resolveToMountTable(ctx, "test/test"));
        assertThat(entry).isNotNull();
        assertThat(entry.getServers()).isNotNull();
        assertThat(entry.getServers()).hasSize(1);
        assertThat(entry.getServers().get(0).getServer()).isEqualTo(mountTableEndpoint.name());
    }

    public void testPermissions() throws Exception {
        AccessList acl = new AccessList(ImmutableList.of(new BlessingPattern("...")),
                ImmutableList.<String>of());
        Namespace n = V.getNamespace(ctx);
        sync(n.mount(ctx, "test/test", dummyServerEndpoint.name(), Duration.standardDays(1)));
        sync(n.setPermissions(ctx, "test/test", new Permissions(ImmutableMap.of("1", acl)), "1"));
        Map<String, Permissions> permissions = sync(n.getPermissions(ctx, "test/test"));
        assertThat(permissions).isNotNull();
        assertThat(permissions).hasSize(1);
        // TODO(sjr): figure out what is actually in this map
        assertThat(permissions).containsKey("2");
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
