// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

import com.google.common.util.concurrent.ListenableFuture;

import io.v.v23.context.VContext;
import io.v.v23.verror.VException;

/**
 * The interface for managing a collection of services.
 */
public interface Server {
    /**
     * Adds the specified name to the mount table for the object or {@link Dispatcher} served by
     * this server.
     *
     * @param  name            name to be added to the mount table
     * @throws VException      if the name couldn't be added to the mount table
     */
    void addName(String name) throws VException;

    /**
     * Removes the specified name from the mount table.
     *
     * @param name name to be removed from the mount table
     */
    void removeName(String name);

    /**
     * Returns a new {@link ListenableFuture} that completes when the server has successfully
     * published all of its endpoints.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context a client context
     * @return        a new listenable future that completes when the server has successfully
     *                published all of its endpoints
     *
     */
    ListenableFuture<Void> allPublished(VContext context);

    /**
     * Returns the current {@link ServerStatus} of the server.
     */
    ServerStatus getStatus();
}
