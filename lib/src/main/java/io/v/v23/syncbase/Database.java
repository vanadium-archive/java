// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.
package io.v.v23.syncbase;

import com.google.common.util.concurrent.ListenableFuture;
import io.v.v23.InputChannel;
import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;
import io.v.v23.services.syncbase.BatchOptions;
import io.v.v23.services.syncbase.BlobRef;
import io.v.v23.services.syncbase.CollectionRowPattern;
import io.v.v23.services.syncbase.Id;
import io.v.v23.services.watch.ResumeMarker;
import io.v.v23.syncbase.util.AccessController;
import io.v.v23.verror.VException;

import javax.annotation.CheckReturnValue;
import java.util.List;

/**
 * A database interface, which is logically a group of {@link Collection}s.
 */
public interface Database extends DatabaseCore, AccessController {
    /**
     * Returns a new {@link ListenableFuture} whose result is {@code true} iff this database exists
     * and the user has sufficient permissions to access it.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     * @return {@code true} iff this database exists and the user has sufficient
     * permissions to access it
     */
    @CheckReturnValue
    ListenableFuture<Boolean> exists(VContext context);

    /**
     * Creates this database.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     * @param perms   database permissions; if {@code null},
     *                {@link io.v.v23.syncbase.SyncbaseApp}'s
     *                permissions are used
     */
    @CheckReturnValue
    ListenableFuture<Void> create(VContext context, Permissions perms);

    /**
     * Destroys this database.
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
     * Creates a new "batch", i.e., a handle to a set of reads and writes to the database that
     * should be considered an atomic unit.  Instead of calling this function directly, clients are
     * encouraged to use the {@link NoSql#runInBatch NoSql.runInBatch()} helper function, which
     * detects "concurrent batch" errors and handles retries internally.
     * <p>
     * Default concurrency semantics are as follows:
     * <ul>
     * <li> Reads (e.g. {@code get}s, {@code scan}s) inside a batch operate over a consistent
     * snapshot taken during {@link #beginBatch beginBatch()}, and will see the effects of prior
     * writes performed inside the batch.</li>
     * <li> {@link BatchDatabase#commit commit()} may throw
     * {@link io.v.v23.services.syncbase.ConcurrentBatchException},
     * indicating that after {@link #beginBatch beginBatch()} but before
     * {@link BatchDatabase#commit commit()}, some concurrent routine wrote to a key that matches
     * a key or row-range read inside this batch.</li>
     * <li> Other methods will never throw
     * {@link io.v.v23.services.syncbase.ConcurrentBatchException},
     * even if it is known that {@link BatchDatabase#commit commit()} will fail with this
     * error.</li>
     * </ul>
     * <p>
     * Once a batch has been committed or aborted, subsequent method calls will
     * fail with no effect.
     * <p>
     * Concurrency semantics can be configured using BatchOptions.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     * @param opts    batch options
     * @return a new {@link ListenableFuture} whose result is a handle to a set of reads
     * and writes to the database that should be considered an atomic unit
     */
    @CheckReturnValue
    ListenableFuture<BatchDatabase> beginBatch(VContext context, BatchOptions opts);

    /**
     * Allows a client to watch for updates to the database. At least one pattern must be specified.
     * For each watch request, the client will receive a reliable {@link InputChannel} of watch
     * events since the provided {@link ResumeMarker} without re-ordering. Only rows matching at
     * least one of the patterns are returned. Rows in collections with no Read access are also
     * filtered out.
     * <p>
     * See {@link io.v.v23.services.watch.GlobWatcherClient} for a detailed explanation of the
     * watch behavior and additional {@link ResumeMarker} semantics.
     * <p>
     * {@link io.v.v23.context.VContext#cancel Canceling} the provided context will
     * stop the watch operation and cause the channel to stop producing elements.  Note that to
     * avoid memory leaks, the caller should drain the channel after cancelling the context.
     *
     * @param context      Vanadium context
     * @param resumeMarker {@link ResumeMarker} from which the changes will be monitored
     * @param patterns     LIKE-style patterns used for filtering returned rows; only rows matching
     * at least one pattern are returned
     * @return a (potentially-infinite) {@link InputChannel} of changes
     */
    InputChannel<WatchChange> watch(VContext context, ResumeMarker resumeMarker, List<CollectionRowPattern> patterns);

    /**
     * Allows a client to watch for updates to the database. Same as
     * {@link #watch(VContext, ResumeMarker, CollectionRowPattern[])} with an empty
     * {@link ResumeMarker}: the first batch on the returned stream represents the initial state of
     * the watched row set at the time of the call.
     *
     * @param context  Vanadium context
     * @param patterns LIKE-style patterns used for filtering returned rows; only rows matching at
     * least one pattern are returned
     * @return a (potentially-infinite) {@link InputChannel} of changes
     */
    InputChannel<WatchChange> watch(VContext context, List<CollectionRowPattern> patterns);

    /**
    * Allows a client to listen for invitations to new syncgroups.
    *
    * @param context Vanadium context
    * @param handler The invitation handler called when an invitation is received.
    */
    void listenForInvites(VContext context, Database.InviteHandler handler) throws VException;

    public interface InviteHandler {
        void handleInvite(Invite invite);
    }

    /**
     * Returns a handle to a database {@link Syncgroup} with the given Id.
     *
     * @param sgId Id of the synchronization group
     */
    Syncgroup getSyncgroup(Id sgId);

    /**
     * Returns a {@link ListenableFuture} whose result is the Id of all
     * {@link Syncgroup}s attached to this database.
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
    ListenableFuture<List<Id>> listSyncgroups(VContext context);

    /**
     * Opens a blob for writing.
     * <p>
     * If invoked with a {@code null} blob reference, a brand new (empty) blob is created.
     * <p>
     * If the blob reference is non-{@code null}, any new writes to the blob are appended to
     * the existing blob data.
     * <p>
     * It is illegal to invoke this method with a reference to an already-committed blob.  If such
     * a reference is passed in, no new writes are applied to the blob;  however, this method may
     * still return a valid {@link BlobWriter} and some of the writes on that writer may
     * <strong>appear</strong> to succeed, though it is not so (see comments on
     * {@link BlobWriter#stream}.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context vanadium context
     * @param ref     blob reference
     * @return a {@link ListenableFuture} whose result is a writer used for writing to
     * the blob
     */
    @CheckReturnValue
    ListenableFuture<BlobWriter> writeBlob(VContext context, BlobRef ref);

    /**
     * Opens a blob for reading.
     * <p>
     * It is illegal to invoke this method with a reference to an un-committed blob.  If such a
     * reference is passed-in, no reads of the blob will succeed, though this method itself
     * may not fail (i.e., it may return a {@link BlobReader} object).
     * <p>
     * This is a non-blocking method.
     *
     * @param context vanadium context
     * @param ref     blob reference
     * @return a {@link ListenableFuture} whose result is a reader used for reading from
     * the blob
     * @throws VException if the blob couldn't be opened for reading
     */
    BlobReader readBlob(VContext context, BlobRef ref) throws VException;

    /**
     * Compares the current schema version of the database with the schema version provided while
     * creating this database handle and updates the schema metadata if required.
     * <p>
     * This method also registers a conflict resolver with syncbase to receive conflicts.
     * <p>
     * Note: this database handle may have been created with a {@code null} schema, in which case
     * this method skips schema check and the caller is responsible for maintaining schema sanity.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param context Vanadium context
     * @return a new {@link ListenableFuture} whose result is {@code true} iff the
     * database schema had to be upgraded, i.e., if the current database schema
     * version was lower than the schema version with which the database was
     * created
     */
    @CheckReturnValue
    ListenableFuture<Void> enforceSchema(VContext context);
}
