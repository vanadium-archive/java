package com.veyron2.ipc;

/**
 * Server defines the interface for managing a collection of services.
 */
public interface Server {
    /**
     * Associates a Dispatcher with a name prefix.  Dispatchers are used
     * in order of the longest prefix matching the name specified in the incoming
     * request.  Multiple dispatchers may be associated with the same prefix
     * (which may be the empty string), in which case they will be invoked in the
     * same order as the invocations of Register.  Path components (the substring
     * between /'s) are not partially matched.
     *
     *   register("media/video", videoSvc)
     *   register("media", mediaSvc)
     * and
     *   register("media", mediaSvc)
     *   register("media/video", videoSvc)
     *
     * will both result in videoSvc being invoked for names of the form
     * "media/video/*"
     *
     * @param  prefix          a name prefix to be associated with the Dispatcher
     * @param  dispatcher      a Dispatcher for handling the provided name prefix.
     * @throws VeyronException if the prefix/Dispatcher couldn't be registered.
     */
    //public void register(String prefix, Dispatcher dispatcher) throws VeyronException;

    /**
     * Creates a listening network endpoint for the Server.  This method may be
     * called multiple times to listen on multiple endpoints.  The returned
     * endpoint represents an address that will be published with the
     * mount table when {@link #publish} is called.
     * TODO(spetrovic): Return an Endpoint object instead of a string.
     *
     * @param  protocol        a network protocol to be used (e.g., "tcp", "bluetooth")
     * @param  address         an address to be used (e.g., "192.168.8.1")
     * @return String          the endpoint string.
     * @throws VeyronException if the provided protocol/address can't be listened on.
     */
    public String listen(String protocol, String address) throws VeyronException;

    /**
     * Enables the services registered thus far to service RPCs.  It
     * will register them with the mount table and maintain that
     * registration so long as {@link #stop()} has not been called.  The name
     * determines where in the mount table's name tree the new services will
     * appear.  The name is applied as a prefix to the prefixes specified in
     * {@link #register}.
     *
     * To serve names of the form "mymedia/media/*" make the calls:
     *   register("media", mediaSvc)
     *   publish("mymedia")
     *
     * This method may be called multiple times to publish the same server under
     * multiple names.
     *
     * TODO(spetrovic): If {@link #listen} hasn't been called yet, it will be called using a
     * default protocol/address?
     *
     * @param  name            name this server should be published under.
     * @throws VeyronException if there was an error publishing.
     */
    public void serve(String name, Dispatcher dispatcher) throws VeyronException;

    /**
     * Gracefully stops all services on this Server.  New calls are
     * rejected, but any in-flight calls are allowed to complete.  All
     * published mountpoints are unmounted.  This call waits for this
     * process to complete, and returns once the server has been shut down.
     *
     * @throws VeyronException if there was an error stopping the server.
     */
    public void stop() throws VeyronException;
}