package io.v.core.veyron2.ipc;

/**
 * ServerState represents the state of the server.
 */
public enum ServerState {
	SERVER_INIT,      // initial state of the server
	SERVER_ACTIVE,    // server is active: listen, serve, addName, or removeName have been called
	SERVER_STOPPING,  // server has been asked to stop and is in the process of doing so
	SERVER_STOPPED    // server has stopped
}