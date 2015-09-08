// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.syncbase.nosql;

import java.lang.reflect.Type;

import io.v.v23.context.VContext;
import io.v.v23.security.access.Permissions;
import io.v.v23.services.syncbase.nosql.PrefixPermissions;
import io.v.v23.verror.VException;

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
     * Create must not be called from within a batch.
     *
     * @param  ctx        Vanadium context
     * @param  perms      table permissions; if {@code null}, {@link Database}'s
     *                    permissions are used
     * @throws VException if the table couldn't be created
     */
    void create(VContext ctx, Permissions perms) throws VException;

    /**
     * Destroys this table, permanently removing all of its data.
     * Destroy must not be called from within a batch.
     *
     * @param  ctx        Vanadium context
     * @throws VException if the table couldn't be deleted
     */
    void destroy(VContext ctx) throws VException;

    /**
     * Returns {@code true} iff this table exists and the caller has sufficient permissions
     * to access it.
     *
     * @param  ctx        Vanadium context
     * @throws VException if the table's existence couldn't be determined
     */
    boolean exists(VContext ctx) throws VException;

    /**
     * Returns the row with the given primary key.
     *
     * @param  key primary key of the row
     */
    Row getRow(String key);

    /**
     * Returns the value for the given primary key.
     * <p>
     * Throws a {@link VException} if the row doesn't exist.
     *
     * @param  ctx        Vanadium context
     * @param  key        the primary key for a row
     * @param  type       type of the value to be returned (needed for de-serialization)
     * @throws VException if the value couldn't be retrieved or if its type doesn't match the
     *                    provided type
     */
    Object get(VContext ctx, String key, Type type) throws VException;

    /**
     * Writes the value to the table under the provided primary key.
     *
     * @param  ctx        Vanadium context
     * @param  key        primary key under which the value is to be written
     * @param  type       type of the value to be returned (needed for serialization)
     * @param  value      value to be written
     * @throws VException if the value couldn't be written
     */
    void put(VContext ctx, String key, Object value, Type type) throws VException;

    /**
     * Deletes the value for the given primary key.
     *
     * @param  ctx        Vanadium context
     * @param  key        primary key for the row to be deleted
     * @throws VException if the row couldn't be deleted
     */
    void delete(VContext ctx, String key) throws VException;

    /**
     * Deletes all rows in the given half-open range {@code [start, limit)}. If {@code limit} is
     * {@code ""}, all rows with keys &ge; {@code start} are deleted.
     *
     * @param  ctx        Vanadium context
     * @param  range      range of rows to be deleted
     * @throws VException if the rows couldn't be deleted
     */
    void deleteRange(VContext ctx, RowRange range) throws VException;

    /**
     * Returns all rows in the given half-open range {@code [start, limit)}. If {@code limit}
     * is {@code ""}, all rows with keys &ge; {@code start} are included.
     * <p>
     * It is legal to perform writes concurrently with {@link #scan scan()}. The returned stream
     * reads from a consistent snapshot taken at the time of the method and will not reflect
     * subsequent writes to keys not yet reached by the stream.
     *
     * @param  ctx         Vanadium context
     * @param  range       range of rows to be read
     * @return             a {@link ScanStream} used for iterating over the snapshot of the
     *                     provided rows
     * @throws VException  if the scan stream couldn't be created
     */
    ScanStream scan(VContext ctx, RowRange range) throws VException;

    /**
     * Returns an array of {@link PrefixPermissions} (i.e., {@code (prefix, perms)} pairs) for
     * the row with the given key.
     * <p>
     * The array is sorted from longest prefix to shortest, so element zero is the one that
     * applies to the row with the given key. The last element is always the prefix {@code ""}
     * which represents the table's permissions -- the array will always have at least one element.
     * <p>
     * TODO(spetrovic): Make a change to VDL so that PrefixPermissions.prefix decodes into a
     * PrefixRange.
     *
     * @param  ctx        Vanadium context
     * @param  key        key of the row whose permission prefixes are to be retrieved
     * @return            an array of prefix permissions for the given row
     * @throws VException if the prefix permissions couldn't be retrieved
     */
    PrefixPermissions[] getPermissions(VContext ctx, String key) throws VException;

    /**
     * Sets the permissions for all current and future rows with the given prefix. If the prefix
     * overlaps with an existing prefix, the longest prefix that matches a row applies.
     * For example:
     * <p><blockquote><pre>
     *     setPermissions(ctx, NoSql.prefix("a/b"), perms1);
     *     setPermissions(ctx, NoSql.prefix("a/b/c"), perms2);
     * </pre></blockquote><p>
     * The permissions for row {@code "a/b/1"} are {@code perms1}, and the permissions for row
     * {@code "a/b/c/1"} are {@code perms2}.
     *
     * @param  ctx        Vanadium context
     * @param  prefix     prefix to which to apply the new permissions
     * @param  perms      permissions to apply
     * @throws VException if the permissions couldn't be applied
     */
    void setPermissions(VContext ctx, PrefixRange prefix, Permissions perms) throws VException;

    /**
     * Deletes permissions for the specified prefix.  Any rows covered by this prefix will use the
     * next longest prefix's permissions.  (See the array returned by
     * {@link #getPermissions getPermissions()}).
     *
     * @param  ctx        Vanadium context
     * @param  prefix     prefix for which the permissions are to be deleted
     * @throws VException if the permissions couldn't be deleted
     */
    void deletePermissions(VContext ctx, PrefixRange prefix) throws VException;
}