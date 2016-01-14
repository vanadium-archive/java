// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase;

import com.google.common.util.concurrent.ListenableFuture;

import io.v.v23.context.VContext;
import io.v.v23.syncbase.util.AccessController;

import java.util.List;

import javax.annotation.CheckReturnValue;

/**
 * The interface for a Vanadium Syncbase service.
 */
public interface SyncbaseService extends AccessController {
    /**
     * Returns the full (i.e., object) name of this service.
     */
    String fullName();

    /**
     * Returns the handle to an app with the given name.
     * <p>
     * Note that this app may not yet exist and can be created using the
     * {@link SyncbaseApp#create} call.
     * <p>
     * This is a non-blocking method.
     *
     * @param  relativeName name of the given app.  May not contain slashes
     * @return              the handle to an app with the given name
     */
    SyncbaseApp getApp(String relativeName);

    /**
     * Returns a {@link ListenableFuture} whose result is a list of all relative app names.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  ctx        Vanadium context
     */
    @CheckReturnValue
    ListenableFuture<List<String>> listApps(VContext ctx);
}