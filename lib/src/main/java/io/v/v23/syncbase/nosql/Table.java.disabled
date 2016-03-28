// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import com.google.common.util.concurrent.ListenableFuture;

import java.lang.reflect.Type;
import java.util.List;

import javax.annotation.CheckReturnValue;

import io.v.v23.InputChannel;
import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;
import io.v.v23.services.syncbase.nosql.KeyValue;
import io.v.v23.services.syncbase.nosql.PrefixPermissions;

/**
 * Interface for a database table, i.e., a collection of {@link Row}s.
 */
public interface Table {
    /**
     * Returns the relative name of this table.
     */
    String name();

    /**
     * Returns the full (i.e., object) name of this table.
     */
    String fullName();

    /**
     * Creates this table.
     * <p>
     * Must not be called from within a batch.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context    Vanadium context
     * @param  perms      table permissions; if {@code null}, {@link Database}'s
     *                    permissions are used
     */
    @CheckReturnValue
    ListenableFuture<Void> create(VContext context, Permissions perms);

    /**
     * Destroys this table, permanently removing all of its data.
     * <p>
     * This method must not be called from within a batch.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context        Vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Void> destroy(VContext context);

    /**
     * Returns a new {@link ListenableFuture} whose result is {@code true} iff this table exists
     * and the caller has sufficient permissions to access it.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context        Vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Boolean> exists(VContext context);

    /**
     * Returns a new {@link ListenableFuture} whose result are the current permissions for the
     * table.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context        Vanadium context
     */
    @CheckReturnValue
    ListenableFuture<Permissions> getPermissions(VContext context);

    /**
     * Replaces the current permissions for the table.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context    Vanadium context
     * @param  perms      new permissions for the table
     */
    @CheckReturnValue
    ListenableFuture<Void> setPermissions(VContext context, Permissions perms);

    /**
     * Returns the row with the given primary key.
     * <p>
     * This is a non-blocking method.
     *
     * @param  key primary key of the row
     */
    Row getRow(String key);

    /**
     * Returns a new {@link ListenableFuture} whose result is the value for the given primary key.
     * <p>
     * The returned future will fail if the row doesn't exist.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context    Vanadium context
     * @param  key        the primary key for a row
     * @param  type       type of the value to be returned (needed for de-serialization)
     */
    @CheckReturnValue
    ListenableFuture<Object> get(VContext context, String key, Type type);

    /**
     * Writes the value to the table under the provided primary key.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context    Vanadium context
     * @param  key        primary key under which the value is to be written
     * @param  type       type of the value to be returned (needed for serialization)
     * @param  value      value to be written
     */
    @CheckReturnValue
    ListenableFuture<Void> put(VContext context, String key, Object value, Type type);

    /**
     * Deletes the value for the given primary key.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context    Vanadium context
     * @param  key        primary key for the row to be deleted
     */
    @CheckReturnValue
    ListenableFuture<Void> delete(VContext context, String key);

    /**
     * Deletes all rows in the given half-open range {@code [start, limit)}. If {@code limit} is
     * {@code ""}, all rows with keys &ge; {@code start} are deleted.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context    Vanadium context
     * @param  range      range of rows to be deleted
     */
    @CheckReturnValue
    ListenableFuture<Void> deleteRange(VContext context, RowRange range);

    /**
     * Returns an {@link InputChannel} over all rows in the given half-open range
     * {@code [start, limit)}. If {@code limit} is {@code ""}, all rows with keys &ge; {@code start}
     * are included.
     * <p>
     * It is legal to perform writes concurrently with {@link #scan scan()}. The returned channel
     * reads from a consistent snapshot taken at the time of the method and will not reflect
     * subsequent writes to keys not yet reached by the stream.
     * <p>
     * {@link io.v.v23.context.VContext#cancel Canceling} the provided context will
     * stop the scan and cause the channel to stop producing elements.  Note that to
     * avoid memory leaks, the caller should drain the channel after cancelling the context.
     *
     * @param  context     Vanadium context
     * @param  range       range of rows to be read
     * @return             an {@link InputChannel} over all rows in the given half-open range
     *                     {@code [start, limit)}
     */
    InputChannel<KeyValue> scan(VContext context, RowRange range);

    /**
     * Returns a new {@link ListenableFuture} whose result is the list of {@link PrefixPermissions}
     * (i.e., {@code (prefix, perms)} pairs) for the row with the given key.
     * <p>
     * The array is sorted from longest prefix to shortest, so element zero is the one that
     * applies to the row with the given key. The last element is always the prefix {@code ""}
     * which represents the table's permissions -- the array will always have at least one element.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     * <p>
     * TODO(spetrovic): Make a change to VDL so that PrefixPermissions.prefix decodes into a
     * PrefixRange.
     *
     * @param  context    Vanadium context
     * @param  key        key of the row whose permission prefixes are to be retrieved
     * @return            a new {@link ListenableFuture} whose result is the list of prefix
     *                    permissions for the given row
     */
    @CheckReturnValue
    ListenableFuture<List<PrefixPermissions>> getPrefixPermissions(VContext context, String key);

    /**
     * Sets the permissions for all current and future rows with the given prefix. If the prefix
     * overlaps with an existing prefix, the longest prefix that matches a row applies.
     * For example:
     * <p><blockquote><pre>
     *     setPrefixPermissions(ctx, NoSql.prefix("a/b"), perms1);
     *     setPrefixPermissions(ctx, NoSql.prefix("a/b/c"), perms2);
     * </pre></blockquote><p>
     * The permissions for row {@code "a/b/1"} are {@code perms1}, and the permissions for row
     * {@code "a/b/c/1"} are {@code perms2}.
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context    Vanadium context
     * @param  prefix     prefix to which to apply the new permissions
     * @param  perms      permissions to apply
     */
    @CheckReturnValue
    ListenableFuture<Void> setPrefixPermissions(
            VContext context, PrefixRange prefix, Permissions perms);

    /**
     * Deletes permissions for the specified prefix.  Any rows covered by this prefix will use the
     * next longest prefix's permissions.  (See the array returned by
     * {@link #getPrefixPermissions getPrefixPermissions()}).
     * <p>
     * The returned future is guaranteed to be executed on an {@link java.util.concurrent.Executor}
     * specified in {@code context} (see {@link io.v.v23.V#withExecutor}).
     * <p>
     * The returned future will fail with {@link java.util.concurrent.CancellationException} if
     * {@code context} gets canceled.
     *
     * @param  context    Vanadium context
     * @param  prefix     prefix for which the permissions are to be deleted
     */
    @CheckReturnValue
    ListenableFuture<Void> deletePrefixPermissions(VContext context, PrefixRange prefix);
}
