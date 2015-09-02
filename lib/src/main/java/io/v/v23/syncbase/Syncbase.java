// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import io.v.impl.google.services.syncbase.syncbased.SyncbaseServer;
import io.v.v23.rpc.Server;

/**
 * Various syncbase utility methods.
 */
public class Syncbase {
    /**
     * Returns a new client handle to a syncbase service running at the given name.
     *
     * @param  fullName full (i.e., object) name of the syncbase service
     */
    public static SyncbaseService newService(String fullName) {
        return new SyncbaseServiceImpl(fullName);
    }

    /**
     * Starts the syncbase server with the given parameters.
     * <p>
     * This is a non-blocking call.
     *
     * @param params                         syncbase starting parameters
     * @throws SyncbaseServerStartException  if there was an error starting the syncbase service
     * @return                               vanadium server
     */
    public static Server startServer(SyncbaseServerParams params)
            throws SyncbaseServerStartException {
        // TODO(spetrovic): allow clients to pass in their own Server implementations.
        return SyncbaseServer.start(params);
    }

    private Syncbase() {}
}
