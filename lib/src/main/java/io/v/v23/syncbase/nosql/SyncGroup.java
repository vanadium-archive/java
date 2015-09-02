// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase.nosql;

import java.util.Map;

import io.v.v23.context.VContext;
import io.v.v23.services.syncbase.nosql.SyncGroupMemberInfo;
import io.v.v23.services.syncbase.nosql.SyncGroupSpec;
import io.v.v23.verror.VException;

/**
 * A handle for a {@link Database} synchronization group.
 */
public interface SyncGroup {
    /**
     * Creates a new sync group with the given spec.  Requires:
     * <p>
     * <ul>
     *     <li> client must have at least {@code read} access on the database,</li>
     *     <li> prefix ACL must exist at each sync group prefix,</li>
     *     <li> client must have at least {@code read} access on each of these prefix ACLs.</li>
     * </ul>
     *
     * @param  ctx        Vanadium context
     * @param  spec       sync group specification
     * @param  info       creator's membership information
     * @throws VException if the sync group couldn't be created
     */
    void create(VContext ctx, SyncGroupSpec spec, SyncGroupMemberInfo info) throws VException;

    /**
     * Joins a sync group.  Requires:
     * <p>
     * <ul>
     *     <li> client must have at least {@code read} access on the database and on the sync group
     *          ACLs.</li>
     * </ul>
     *
     * @param  ctx        Vanadium context
     * @param  info       joiner's membership information
     * @return            sync group specification
     * @throws VException if the sync group couldn't be joined
     */
    SyncGroupSpec join(VContext ctx, SyncGroupMemberInfo info) throws VException;

    /**
     * Leaves the sync group.  Previously synced data will continue to be available.  Requires:
     * <p>
     * <ul>
     *     <li> client must have at least {@code read} access on the database.</li>
     * </ul>
     *
     * @param  ctx        Vanadium context
     * @throws VException if the sync group couldn't be left
     */
    void leave(VContext ctx) throws VException;

    /**
     * Destroys the sync group.  Previously synced data will continue to be available to all
     * members.  Requires:
     * <p>
     * <ul>
     *     <li> client must have at least {@code read} access on the database and an {@code admin}
     *          access on the sync group ACLs.</li>
     * </ul>
     *
     * @param  ctx        Vanadium context
     * @throws VException if the sync group couldn't be destroyed
     */
    void destroy(VContext ctx) throws VException;

    /**
     * Ejects a member from the sync group.  The ejected member will not be able to sync further,
     * but will retain any data it has already synced.  Requires:
     * <p>
     * <ul>
     *     <li> client must have at least {@code read} access on the database and an {@code admin}
     *          access on the sync group ACLs.</li>
     * </ul>
     *
     * @param  ctx        Vanadium context
     * @param  member     member to be ejected
     * @throws VException if the member couldn't be ejected
     */
    void eject(VContext ctx, String member) throws VException;

    /**
     * Returns the sync group specification, along with its version string.  The version number
     * allows for atomic read-modify-write of the specification (see {@link #setSpec setSpec()}).
     * Requires:
     * <p>
     * <ul>
     *     <li> client must have at least {@code read} access on the database and on the sync group
     *          ACLs.</li>
     * </ul>
     *
     * @param  ctx        Vanadium context
     * @return            sync group specification along with its version number.  The returned
     *                    map is guaranteed to be non-{@code null} and contain exactly one element
     * @throws VException if the sync group specification couldn't be retrieved
     */
    Map<String, SyncGroupSpec> getSpec(VContext ctx) throws VException;

    /**
     * Sets the sync group specification.  The version string may be either empty or the value of
     * the previous {@link #getSpec getSpec()}, in which case {@link #setSpec setSpec()} will only
     * succeed iff the current version matches the specified one.
     * <p>
     * <ul>
     *     <li> client must have at least {@code read} access on the database and an {@code admin}
     *          access on the sync group ACLs.</li>
     * </ul>
     *
     * @param  ctx        Vanadium context
     * @param  spec       the new sync group specification
     * @param  version    expected version string of the sync group;  if non-empty, this
     *                    method will only succeed if the current sync group's version matches
     *                    this value
     * @throws VException if the sync group specification couldn't be set
     */
    void setSpec(VContext ctx, SyncGroupSpec spec, String version) throws VException;

    /**
     * Returns the information on membership of the sync group.  Requires:
     * <p>
     * <ul>
     *     <li> client must have at least {@code read} access on the database and on the sync group
     *          ACLs.</li>
     * </ul>
     *
     * @param  ctx        Vanadium context
     * @throws VException [description]
     */
    Map<String, SyncGroupMemberInfo> getMembers(VContext ctx) throws VException;
}