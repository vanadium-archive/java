// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase.util;

import java.util.Map;

import io.v.v23.context.VContext;
import io.v.v23.rpc.Callback;
import io.v.v23.security.access.Permissions;
import io.v.v23.verror.VException;

/**
 * Provides access control for various syncbase objects.
 */
public interface AccessController {
    /**
     * Replaces the current permissions for an object.
     * <p>
     * For a detailed documentation, see
     * {@link io.v.v23.services.permissions.ObjectClient#setPermissions}.
     *
     * @param  ctx        Vanadium context
     * @param  perms      new permissions for the object
     * @param  version    object's permissions version, which allows for optional, optimistic
     *                    concurrency control.  If non-empty, this value must come from
     *                    {@link #getPermissions getPermissions()}.  If empty,
     *                    {@link #setPermissions setPermissions()} performs an unconditional update.
     * @throws VException if the object permissions couldn't be updated
     */
    void setPermissions(VContext ctx, Permissions perms, String version) throws VException;

    /**
     * Asynchronous version of {@link #setPermissions(VContext, Permissions, String)}.
     *
     * @throws VException if there was an error creating the asynchronous call. In this case, no
     *                    methods on {@code callback} will be called.
     */
    void setPermissions(VContext ctx, Permissions perms, String version, Callback<Void> callback)
            throws VException;

    /**
     * Returns the current permissions for an object.
     * <p>
     * For detailed documentation, see
     * {@link io.v.v23.services.permissions.ObjectClient#getPermissions}.
     *
     * @param  ctx        Vanadium context
     * @return            object permissions along with its version number.  The returned map
     *                    is guaranteed to be non-{@code null} and contain exactly one element
     * @throws VException if the object permissions couldn't be retrieved
     */
    Map<String, Permissions> getPermissions(VContext ctx) throws VException;

    /**
     * Asynchronous version of {@link #getPermissions(VContext)}.
     *
     * @throws VException if there was an error creating the asynchronous call. In this case, no
     *                    methods on {@code callback} will be called.
     */
    void getPermissions(VContext ctx, Callback<Map<String, Permissions>> callback)
            throws VException;
}