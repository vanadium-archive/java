package io.v.v23.rpc;

import io.v.v23.security.Blessings;

/**
 * ServerCall defines the in-flight call state on the server, not including methods
 * to stream args and results.
 */
public interface ServerCall extends io.v.v23.security.Call {
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
    public Blessings blessings();
    
    // TODO(spetrovic): Add the server() method.
}