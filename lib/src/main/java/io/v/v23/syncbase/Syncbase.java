// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase;

import com.google.common.base.CharMatcher;
import io.v.impl.google.services.syncbase.syncbased.SyncbaseServer;
import io.v.v23.context.VContext;

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
        return SyncbaseServer.withNewServer(ctx, params);
    }

    /**
     * Returns {@code true} iff the provided string is a valid syncbase name.
     */
    public static boolean isValidName(String name) {
        return !name.isEmpty() && CharMatcher.JAVA_LETTER_OR_DIGIT.matchesAllOf(name);
    }

    private Syncbase() {}
}
