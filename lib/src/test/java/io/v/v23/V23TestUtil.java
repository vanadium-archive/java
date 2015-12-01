// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23;

import io.v.v23.context.VContext;
import io.v.v23.naming.Endpoint;
import io.v.v23.rpc.ListenSpec;
import io.v.v23.rpc.Server;
import io.v.v23.rpc.ServerCall;
import io.v.v23.security.VSecurity;
import io.v.v23.security.access.Permissions;
import io.v.v23.services.permissions.ObjectServer;
import io.v.v23.verror.VException;

import static com.google.common.truth.Truth.assertThat;

/**
 * Various Vanadium test utilities.
 */
public class V23TestUtil {
    public static VContext withDummyServer(VContext ctx) throws Exception {
        ctx = V.withListenSpec(ctx, V.getListenSpec(ctx).withAddress(
                new ListenSpec.Address("tcp", "localhost:0")));
        return V.withNewServer(ctx, "", new ObjectServer() {
                    @Override
                    public void setPermissions(VContext ctx, ServerCall call,
                                               Permissions permissions, String version)
                            throws VException {
                        throw new VException("Unimplemented!");
                    }
                    @Override
                    public ObjectServer.GetPermissionsOut getPermissions(
                            VContext ctx, ServerCall call) throws VException {
                        throw new VException("Unimplemented!");
                    }
                },
                VSecurity.newAllowEveryoneAuthorizer());
    }

    /**
     * Returns the first available endpoint for a server attached to the given context.
     */
    public static Endpoint getServerEndpoint(VContext ctx) {
        Server server = V.getServer(ctx);
        assertThat(server).isNotNull();
        assertThat(server.getStatus()).isNotNull();
        assertThat(server.getStatus().getEndpoints()).isNotEmpty();
        return server.getStatus().getEndpoints()[0];
    }
}
