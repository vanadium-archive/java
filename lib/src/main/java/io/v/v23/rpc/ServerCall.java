// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;

/**
 * ServerCall defines the in-flight call state on the server, not including methods
 * to stream args and results.
 */
public interface ServerCall {
    /**
     * Returns the object name suffix for the request.
     */
    String suffix();

    /**
     * Returns the Endpoint at the local end of communication.
     */
    String localEndpoint();

    /**
     * Returns the Endpoint at the remote end of communication.
     */
    String remoteEndpoint();

    /**
     * Returns blessings bound to the server's private key (technically, the server principal's
     * private key) provided by the client of the RPC.
     *
     * This method can return {@code null}, which indicates that the client did not provide any
     * blessings to the server with the request.
     *
     * Note that these blessings are distinct from the blessings used by the client and
     * server to authenticate with each other (RemoteBlessings and LocalBlessings respectively).
     *
     * @return blessings bound to the server's private key.
     */
    Blessings grantedBlessings();

    /**
     * Returns the {@link Server} that this call is associated with.
     */
    Server server();
}