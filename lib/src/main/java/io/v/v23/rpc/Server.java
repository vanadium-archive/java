// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

import io.v.v23.InputChannel;
import io.v.v23.security.Authorizer;
import io.v.v23.verror.VException;

/**
 * The interface for managing a collection of services.
 */
public interface Server {
    /**
     * Creates a listening network endpoint for the server as specified by the provided
     * {@link ListenSpec} parameter.
     * <p>
     * Returns the set of endpoints that can be used to reach this server.  A single listen
     * address in the listen spec can lead to multiple such endpoints (e.g. {@code :0} on a device
     * with multiple interfaces or that is being proxied). In the case where multiple listen
     * addresses are used it is not possible to tell which listen address supports which endpoint;
     * if there is need to associate endpoints with specific listen addresses then this method
     * should be called separately for each one.
     *
     * @param  spec            information on how to create the network endpoint(s)
     * @return                 endpoints the server is listening on
     * @throws VException      if the server couldn't listen provided protocol can't be listened on
     */
    String[] listen(ListenSpec spec) throws VException;

    /**
     * Associates object with name by publishing the address of this server with the mount table
     * under the supplied name and using the given authorizer to authorize access to it.  RPCs
     * invoked on the supplied name will be delivered to methods implemented by the supplied object.
     * <p>
     * Reflection is used to match requests to the object's method set.  As a special-case, if the
     * object implements the {@link Invoker} interface, the invoker is used to invoke methods
     * directly, without reflection.
     * <p>
     * If name is an empty string, no attempt will made to publish that name to a mount table.
     * <p>
     * If the passed-in authorizer in {@code null}, the default authorizer will be used.  (The
     * default authorizer uses the blessing chain derivation to determine if the client is
     * authorized to access the object's methods.)
     * <p>
     * It is an error to call this method if {@link #serveDispatcher serveDispatcher} has already
     * been called. It is also an error to call this method multiple times.  It is considered an
     * error to call {@link #listen listen} after calling this method.
     *
     * @param  name            name under which the supplied object should be published,
     *                         or the empty string if the object should not be published
     * @param  object          object to be published under the given name
     * @throws VException      if the object couldn't be published under the given name
     */
    void serve(String name, Object object, Authorizer auth) throws VException;

    /**
     * Associates dispatcher with the portion of the mount table's name space for which {@code name}
     * is a prefix, by publishing the address of this dispatcher with the mount table under the
     * supplied name.
     * <p>
     * RPCs invoked on the supplied name will be delivered to the supplied {@link Dispatcher}'s
     * {@link Dispatcher#lookup lookup} method which will in turn return the object and
     * {@link Authorizer} used to serve the actual RPC call.
     * <p>
     * If name is an empty string, no attempt will made to publish that name to a mount table.
     * <p>
     * It is an error to call this method if {@link #serve serve} has already been called.
     * It is also an error to call this method multiple times.  It is considered an
     * error to call {@link #listen listen} after calling this method.
     *
     * @param  name            name under which the dispatcher should be published, or the empty
     *                         string if the dispatcher should not be published
     * @param  dispatcher      dispatcher to be published under the given name
     * @throws VException      if the dispatcher couldn't be published under the given name
     */
    void serveDispatcher(String name, Dispatcher disp) throws VException;

    /**
     * Adds the specified name to the mount table for the object or {@link Dispatcher} served by
     * this server.
     * <p>
     * This method may be called multiple times but only after {@link #serve serve} or
     * {@link #serveDispatcher serveDispatcher} has been called.
     *
     * @param  name            name to be added to the mount table
     * @throws VException      if the name couldn't be added to the mount table
     */
    void addName(String name) throws VException;

    /**
     * Removes the specified name from the mount table.
     * <p>
     * This method may be called multiple times but only after {@link #serve serve} or
     * {@link #serveDispatcher serveDispatcher} has been called.
     *
     * @param name name to be removed from the mount table
     */
    void removeName(String name);

    /**
     * Returns the current {@link ServerStatus} of the server.
     */
    ServerStatus getStatus();

    /**
     * Returns a channel over which {@link NetworkChange}s will be sent. The server will
     * not block sending data over this channel and hence change events may be lost if the
     * implementation doesn't ensure there is sufficient buffering in the channel.
     */
    InputChannel<NetworkChange> watchNetwork();

    /**
     * Unregisters a channel previously registered using {@link #watchNetwork}.
     *
     * @param channel a channel previously registered using {@link #watchNetwork}
     */
    void unwatchNetwork(InputChannel<NetworkChange> channel);

    /**
     * Gracefully stops all services on this server.  New calls are rejected, but any in-flight
     * calls are allowed to complete.  All published mountpoints are unmounted.  This call waits for
     * this process to complete and returns once the server has been shut down.
     *
     * @throws VException      if there was an error stopping the server
     */
    void stop() throws VException;
}