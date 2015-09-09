// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.services.syncbase.syncbased;

import io.v.v23.context.VContext;
import io.v.v23.rpc.Server;
import io.v.v23.syncbase.SyncbaseServerParams;
import io.v.v23.syncbase.SyncbaseServerStartException;
import io.v.v23.verror.VException;

/**
 * An implementation of a syncbase server.
 */
public class SyncbaseServer {
    private static native Server nativeStart(VContext ctx, SyncbaseServerParams params)
            throws VException;

    /**
     * Starts the syncbase server with the given parameters.
     * <p>
     * This is a non-blocking call.
     *
     * @param ctx                            vanadium context
     * @param params                         syncbase starting parameters
     * @throws SyncbaseServerStartException  if there was an error starting the syncbase service
     * @return                               vanadium server
     */
    public static Server start(VContext ctx, SyncbaseServerParams params)
            throws SyncbaseServerStartException {
        try {
            return nativeStart(ctx, params);
        } catch (VException e) {
            throw new SyncbaseServerStartException(e.getMessage());
        }
    }
}
