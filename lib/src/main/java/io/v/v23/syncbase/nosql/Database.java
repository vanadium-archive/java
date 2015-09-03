// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase.nosql;

import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;
import io.v.v23.services.syncbase.nosql.BatchOptions;
import io.v.v23.syncbase.util.AccessController;
import io.v.v23.verror.VException;

/**
 * A database interface, which is logically a collection of {@link Table}s.
 */
public interface Database extends DatabaseCore, AccessController {
    /**
     * Returns {@code true} iff this database exists and the user has sufficient
     * permissions to access it.
     *
     * @param  ctx        Vanadium context
     * @return            {@code true} iff this database exists and the user has sufficient
     *                    permissions to access it
     * @throws VException if the database's existence couldn't be determined
     */
    boolean exists(VContext ctx) throws VException;

    /**
     * Creates this database.
     *
     * @param  ctx        Vanadium context
     * @param  perms      database permissions; if {@code null},
     *                    {@link io.v.v23.syncbase.SyncbaseApp}'s
     *                    permissions are used
     * @throws VException if the database couldn't be created
     */
    void create(VContext ctx, Permissions perms) throws VException;

    /**
     * Destroys this database.
     *
     * @param  ctx        Vanadium context
     * @throws VException if the database couldn't be destroyed
     */
    void destroy(VContext ctx) throws VException;

    /**
     * Creates the table with the given name.
     *
     * @param  ctx           Vanadium context
     * @param  relativeName  relative name of the table; must not contain {@code /}
     * @param  perms         table permissions; if {@code null}, {@link Database}'s
     *                       permissions are used
     * @throws VException    if the table couldn't be created
     */
    void createTable(VContext ctx, String relativeName, Permissions perms) throws VException;

    /**
     * Deletes the table with the given name.
     *
     * @param  ctx           Vanadium context
     * @param  relativeName  relative name of the table; must not contain {@code /}
     * @throws VException    if the table couldn't be deleted
     */
    void deleteTable(VContext ctx, String relativeName) throws VException;

    /**
     * Creates a new "batch", i.e., a handle to a set of reads and writes to the database that
     * should be considered an atomic unit.  Instead of calling this function directly, clients are
     * encouraged to use the {@link NoSql#runInBatch NoSql.runInBatch()} helper function, which
     * detects "concurrent batch" errors and handles retries internally.
     * <p>
     * Default concurrency semantics are as follows:
     * <ul>
     *   <li> Reads (e.g. {@code get}s, {@code scan}s) inside a batch operate over a consistent
     *   snapshot taken during {@link #beginBatch beginBatch()}, and will see the effects of prior
     *   writes performed inside the batch.</li>
     *   <li> {@link BatchDatabase#commit commit()} may fail with
     *   {@link io.v.v23.services.syncbase.nosql.Errors#CONCURRENT_BATCH CONCURRENT_BATCH},
     *   indicating that after {@link #beginBatch beginBatch()} but before
     *   {@link BatchDatabase#commit commit()}, some concurrent routine wrote to a key that matches
     *   a key or row-range read inside this batch.</li>
     *   <li> Other methods will never fail with error
     *   {@link io.v.v23.services.syncbase.nosql.Errors#CONCURRENT_BATCH CONCURRENT_BATCH},
     *   even if it is known that {@link BatchDatabase#commit commit()} will fail with this
     *   error.</li>
     * </ul>
     * <p>
     * Once a batch has been committed or aborted, subsequent method calls will
     * fail with no effect.
     * <p>
     * Concurrency semantics can be configured using BatchOptions.
     *
     * @param  ctx        Vanadium context
     * @param  opts       batch options
     * @return            a handle to a set of reads and writes to the database that should be
     *                    considered an atomic unit
     * @throws VException if the batch couldn't be created
     */
    BatchDatabase beginBatch(VContext ctx, BatchOptions opts) throws VException;

    /**
     * Returns a handle to a database {@link SyncGroup} with the given full (i.e., object) name.
     *
     * @param  name name of the synchronization group
     */
    SyncGroup getSyncGroup(String name);

    /**
     * Returns the global names of all {@link SyncGroup}s attached to this database.
     *
     * @param  ctx        Vanadium context
     * @throws VException if the sync group names couldn't be retrieved
     */
    String[] listSyncGroupNames(VContext ctx) throws VException;

    /**
     * Compares the current schema version of the database with the schema version provided while
     * creating this database handle.  If the current database schema version is lower, then the
     * {@link SchemaUpgrader} associated with the schema is called. If {@link SchemaUpgrader} is
     * successful, this method stores the new schema metadata in database.
     * <p>
     * Note: this database handle may have been created with a {@code null} schema, in which case
     * this method skips schema check and the caller is responsible for maintaining schema sanity.
     *
     * @param  ctx        Vanadium context
     * @return            {@code true} iff the database schema had to be upgraded, i.e., if the
     *                    current database schema version was lower than the schema version with
     *                    which the database was created
     * @throws VException if there was an error upgrading the schema
     */
    boolean upgradeIfOutdated(VContext ctx) throws VException;
}