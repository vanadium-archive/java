// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase;

import com.google.common.util.concurrent.ListenableFuture;
import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.SyncgroupMemberInfo;
import io.v.v23.services.syncbase.SyncgroupSpec;

import javax.annotation.CheckReturnValue;

import java.util.List;
import java.util.Map;

/**
 * A handle for a {@link Database} synchronization group.
 */
public interface Syncgroup {
    /**
     * Creates a new syncgroup with the given spec.  Requires:
     * <p>
     * <ul>
     * <li> client must have at least {@code read} access on the database,</li>
     * <li> prefix ACL must exist at each syncgroup prefix,</li>
     * <li> client must have at least {@code read} access on each of these prefix ACLs.</li>
     * </ul>
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     * @param spec    syncgroup specification
     * @param info    creator's membership information
     */
    @CheckReturnValue
    ListenableFuture<Void> create(VContext context, SyncgroupSpec spec, SyncgroupMemberInfo info);

    /**
     * Joins a syncgroup.  Requires:
     * <p>
     * <ul>
     * <li> client must have at least {@code read} access on the database and on the syncgroup
     * ACLs.</li>
     * </ul>
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     * @param remoteSyncbaseName the name of another syncbase that has the syncgroup we want to join
     * @param expectedSyncbaseBlessings the blessings that the remote syncbase must have
     * @param info    joiner's membership information
     * @return a new {@link ListenableFuture} whose result is the syncgroup specification
     */
    @CheckReturnValue
    ListenableFuture<SyncgroupSpec> join(VContext context, String remoteSyncbaseName, List<String> expectedSyncbaseBlessings, SyncgroupMemberInfo info);

    /**
     * Leaves the syncgroup.  Previously synced data will continue to be available.  Requires:
     * <p>
     * <ul>
     * <li> client must have at least {@code read} access on the database.</li>
     * </ul>
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Void> leave(VContext context);

    /**
     * Destroys the syncgroup.  Previously synced data will continue to be available to all
     * members.  Requires:
     * <p>
     * <ul>
     * <li> client must have at least {@code read} access on the database and an {@code admin}
     * access on the syncgroup ACLs.</li>
     * </ul>
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Void> destroy(VContext context);

    /**
     * Ejects a member from the syncgroup.  The ejected member will not be able to sync further,
     * but will retain any data it has already synced.  Requires:
     * <p>
     * <ul>
     * <li> client must have at least {@code read} access on the database and an {@code admin}
     * access on the syncgroup ACLs.</li>
     * </ul>
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     * @param member  member to be ejected
     */
    @CheckReturnValue
    ListenableFuture<Void> eject(VContext context, String member);

    /**
     * Returns a new {@link ListenableFuture} whose result is the syncgroup specification, along
     * with its version string.  The version number allows for atomic read-modify-write of the
     * specification (see {@link #setSpec setSpec()}). Requires:
     * <p>
     * <ul>
     * <li> client must have at least {@code read} access on the database and on the syncgroup
     * ACLs.</li>
     * </ul>
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     * @return a new {@link ListenableFuture} whose result is the syncgroup specification
     * along with its version number; the returned map is guaranteed to be
     * non-{@code null} and contain exactly one element
     */
    @CheckReturnValue
    ListenableFuture<Map<String, SyncgroupSpec>> getSpec(VContext context);

    /**
     * Sets the syncgroup specification.  The version string may be either empty or the value of
     * the previous {@link #getSpec getSpec()}, in which case {@link #setSpec setSpec()} will only
     * succeed iff the current version matches the specified one.
     * <p>
     * <ul>
     * <li> client must have at least {@code read} access on the database and an {@code admin}
     * access on the syncgroup ACLs.</li>
     * </ul>
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     * @param spec    the new syncgroup specification
     * @param version expected version string of the syncgroup;  if non-empty, this
     *                method will only succeed if the current syncgroup's version matches
     *                this value
     */
    @CheckReturnValue
    ListenableFuture<Void> setSpec(VContext context, SyncgroupSpec spec, String version);

    /**
     * Returns a new {@link ListenableFuture} whose result is the membership of the syncgroup.
     * Requires:
     * <p>
     * <ul>
     * <li> client must have at least {@code read} access on the database and on the syncgroup
     * ACLs.</li>
     * </ul>
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Map<String, SyncgroupMemberInfo>> getMembers(VContext context);
}
