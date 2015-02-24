package io.v.v23.ipc;

/**
 * ServerCall defines the interface for each in-flight call on the server.
 */
public interface ServerCall extends Stream, ServerContext {}