// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase.nosql;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.Map;

import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.nosql.SyncgroupMemberInfo;
import io.v.v23.services.syncbase.nosql.SyncgroupSpec;
import io.v.v23.verror.VException;

/**
 * A handle for a {@link Database} synchronization group.
 */
public interface Syncgroup {
    /**
     * Creates a new syncgroup with the given spec.  Requires:
     * <p>
     * <ul>
     *     <li> client must have at least {@code read} access on the database,</li>
     *     <li> prefix ACL must exist at each syncgroup prefix,</li>
     *     <li> client must have at least {@code read} access on each of these prefix ACLs.</li>
     * </ul>
     *
     * @param  ctx        Vanadium context
     * @param  spec       syncgroup specification
     * @param  info       creator's membership information
     */
    ListenableFuture<Void> create(VContext ctx, SyncgroupSpec spec, SyncgroupMemberInfo info);

    /**
     * Joins a syncgroup.  Requires:
     * <p>
     * <ul>
     *     <li> client must have at least {@code read} access on the database and on the syncgroup
     *          ACLs.</li>
     * </ul>
     *
     * @param  ctx        Vanadium context
     * @param  info       joiner's membership information
     * @return            a new {@link ListenableFuture} whose result is the syncgroup specification
     */
    ListenableFuture<SyncgroupSpec> join(VContext ctx, SyncgroupMemberInfo info);

    /**
     * Leaves the syncgroup.  Previously synced data will continue to be available.  Requires:
     * <p>
     * <ul>
     *     <li> client must have at least {@code read} access on the database.</li>
     * </ul>
     *
     * @param  ctx        Vanadium context
     */
    ListenableFuture<Void> leave(VContext ctx);

    /**
     * Destroys the syncgroup.  Previously synced data will continue to be available to all
     * members.  Requires:
     * <p>
     * <ul>
     *     <li> client must have at least {@code read} access on the database and an {@code admin}
     *          access on the syncgroup ACLs.</li>
     * </ul>
     *
     * @param  ctx        Vanadium context
     */
    ListenableFuture<Void> destroy(VContext ctx);

    /**
     * Ejects a member from the syncgroup.  The ejected member will not be able to sync further,
     * but will retain any data it has already synced.  Requires:
     * <p>
     * <ul>
     *     <li> client must have at least {@code read} access on the database and an {@code admin}
     *          access on the syncgroup ACLs.</li>
     * </ul>
     *
     * @param  ctx        Vanadium context
     * @param  member     member to be ejected
     */
    ListenableFuture<Void> eject(VContext ctx, String member);

    /**
     * Returns a new {@link ListenableFuture} whose result is the syncgroup specification, along
     * with its version string.  The version number allows for atomic read-modify-write of the
     * specification (see {@link #setSpec setSpec()}). Requires:
     * <p>
     * <ul>
     *     <li> client must have at least {@code read} access on the database and on the syncgroup
     *          ACLs.</li>
     * </ul>
     *
     * @param  ctx        Vanadium context
     * @return            a new {@link ListenableFuture} whose result is the syncgroup specification
     *                    along with its version number; the returned map is guaranteed to be
     *                    non-{@code null} and contain exactly one element
     */
    ListenableFuture<Map<String, SyncgroupSpec>> getSpec(VContext ctx);

    /**
     * Sets the syncgroup specification.  The version string may be either empty or the value of
     * the previous {@link #getSpec getSpec()}, in which case {@link #setSpec setSpec()} will only
     * succeed iff the current version matches the specified one.
     * <p>
     * <ul>
     *     <li> client must have at least {@code read} access on the database and an {@code admin}
     *          access on the syncgroup ACLs.</li>
     * </ul>
     *
     * @param  ctx        Vanadium context
     * @param  spec       the new syncgroup specification
     * @param  version    expected version string of the syncgroup;  if non-empty, this
     *                    method will only succeed if the current syncgroup's version matches
     *                    this value
     */
    ListenableFuture<Void> setSpec(VContext ctx, SyncgroupSpec spec, String version);

    /**
     * Returns a new {@link ListenableFuture} whose result is the membership of the syncgroup.
     * Requires:
     * <p>
     * <ul>
     *     <li> client must have at least {@code read} access on the database and on the syncgroup
     *          ACLs.</li>
     * </ul>
     *
     * @param  ctx        Vanadium context
     */
    ListenableFuture<Map<String, SyncgroupMemberInfo>> getMembers(VContext ctx);
}
