// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.services.syncbase.syncbased;

import io.v.v23.context.VContext;
import io.v.v23.syncbase.SyncbaseServerParams;
import io.v.v23.syncbase.SyncbaseServerStartException;
import io.v.v23.verror.VException;

/**
 * An implementation of a syncbase server.
 */
public class SyncbaseServer {
    private static native VContext nativeWithNewServer(
            VContext ctx, SyncbaseServerParams params) throws VException;

    /**
     * Creates a new the syncbase server and attaches it to a new context (which is derived
     * from the provided context).
     * <p>
     * The newly created {@link io.v.v23.rpc.Server} instance can be obtained from the context via
     * {@link io.v.v23.V#getServer}.
     *
     * @param  ctx                           vanadium context
     * @param  params                        syncbase starting parameters
     * @throws SyncbaseServerStartException  if there was an error starting the syncbase service
     * @return                               a child context to which the new server is attached
     */
    public static VContext withNewServer(VContext ctx, SyncbaseServerParams params)
            throws SyncbaseServerStartException {
        try {
            return nativeWithNewServer(ctx, params);
        } catch (VException e) {
            throw new SyncbaseServerStartException(e.getMessage());
        }
    }
}
