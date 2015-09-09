// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import com.google.common.base.CharMatcher;
import io.v.impl.google.services.syncbase.syncbased.SyncbaseServer;
import io.v.v23.context.VContext;
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
     * @param ctx                            vanadium context
     * @param params                         syncbase starting parameters
     * @throws SyncbaseServerStartException  if there was an error starting the syncbase service
     * @return                               vanadium server
     */
    public static Server startServer(VContext ctx, SyncbaseServerParams params)
            throws SyncbaseServerStartException {
        // TODO(spetrovic): allow clients to pass in their own Server implementations.
        return SyncbaseServer.start(ctx, params);
    }

    /**
     * Returns {@code true} iff the provided string is a valid syncbase name.
     */
    public static boolean isValidName(String name) {
        return !name.isEmpty() && CharMatcher.JAVA_LETTER_OR_DIGIT.matchesAllOf(name);
    }

    private Syncbase() {}
}
