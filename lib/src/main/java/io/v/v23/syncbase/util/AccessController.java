// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase.util;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Map;

import javax.annotation.CheckReturnValue;

import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;

/**
 * Provides access control for various syncbase objects.
 */
public interface AccessController {
    /**
     * Replaces the current permissions for an object.
     * <p>
     * For a detailed documentation, see
     * {@link io.v.v23.services.permissions.ObjectClient#setPermissions}.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context    Vanadium context
     * @param  perms      new permissions for the object
     * @param  version    object's permissions version, which allows for optional, optimistic
     *                    concurrency control.  If non-empty, this value must come from
     *                    {@link #getPermissions getPermissions()}.  If empty,
     *                    {@link #setPermissions setPermissions()} performs an unconditional update.
     */
    @CheckReturnValue
    ListenableFuture<Void> setPermissions(VContext context, Permissions perms, String version);

    /**
     * Returns a new {@link ListenableFuture} whose result are the current permissions for
     * an object.
     * <p>
     * For detailed documentation, see
     * {@link io.v.v23.services.permissions.ObjectClient#getPermissions}.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context        Vanadium context
     * @return            a new {@link ListenableFuture} whose result are object's permissions along
     *                    with its version number;  the result map is guaranteed to be
     *                    non-{@code null} and contain exactly one element
     */
    @CheckReturnValue
    ListenableFuture<Map<String, Permissions>> getPermissions(VContext context);
}
