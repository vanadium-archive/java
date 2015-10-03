// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.namespace;

import io.v.v23.rpc.Callback;
import org.joda.time.Duration;

import java.util.List;
import java.util.Map;

import io.v.v23.Options;
import io.v.v23.context.VContext;
import io.v.v23.naming.GlobReply;
import io.v.v23.naming.MountEntry;
import io.v.v23.security.access.Permissions;
import io.v.v23.verror.VException;

/**
 * Translation from object names to server object addresses.  It represents the interface to a
 * client side library for the {@code MountTable} service.
 */
public interface Namespace {
    /**
     * A shortcut for {@link #mount(VContext, String, String, Duration, Options)} with a {@code
     * null} options parameter.
     */
    void mount(VContext context, String name, String server, Duration ttl) throws VException;

    /**
     * Mounts the server object address under the object name, expiring after {@code ttl}. {@code
     * ttl} of zero implies an implementation-specific high value (essentially forever).
     * <p>
     * A particular implementation of this interface chooses which options to support,
     * but at the minimum it must handle the following pre-defined options:
     * <ul>
     *     <li>{@link io.v.v23.OptionDefs#SKIP_SERVER_ENDPOINT_AUTHORIZATION}</li>
     * </ul>
     *
     * @param context a client context
     * @param name a Vanadium name, see also <a href="https://github.com/vanadium/docs/blob/master/glossary.md#object-name">the
     *             Name entry</a> in the glossary
     * @param server an object address, see also
     *               <a href="https://github.com/vanadium/docs/blob/master/concepts/naming.md#object-names">the Object names</a>
     *               section of the Naming Concepts document
     * @param ttl the duration for which the mount should live
     * @param options options to pass to the implementation as described above, or {@code null}
     * @throws VException if the server object address could not be mounted
     */
    void mount(VContext context, String name, String server, Duration ttl, Options options)
            throws VException;

    /**
     * A shortcut for {@link #mount(VContext, String, String, Duration, Options, Callback)} with
     * a {@code null} options parameter.
     */
    void mount(VContext context, String name, String server, Duration ttl, Callback<Void> callback)
            throws VException;

    /**
     * Asynchronous version of {@link #mount(VContext, String, String, Duration, Options)} that
     * takes in a callback whose {@code onSuccess} method will be called when the operation
     * completes successfully, and whose {@code onFailure} will be called if an error is
     * encountered.
     */
    void mount(VContext context, String name, String server, Duration ttl, Options options,
               Callback<Void> callback) throws VException;

    /**
     * A shortcut for {@link #unmount(VContext, String, String, Options)} with a {@code null}
     * options parameter.
     */
    void unmount(VContext context, String name, String server) throws VException;

    /**
     * Unmounts the server object address from the object name, or if {@code server} is empty,
     * unmounts all server object addresses from the object name.
     * <p>
     * A particular implementation of this interface chooses which options to support,
     * but at the minimum it must handle the following pre-defined options:
     * <ul>
     *     <li>{@link io.v.v23.OptionDefs#SKIP_SERVER_ENDPOINT_AUTHORIZATION}</li>
     * </ul>
     *
     * @param context a client context
     * @param name a Vanadium name, see also <a href="https://github.com/vanadium/docs/blob/master/glossary.md#object-name">the
     *             Name entry</a> in the glossary
     * @param server an object address, see also
     *               <a href="https://github.com/vanadium/docs/blob/master/concepts/naming.md#object-names">the Object names</a>
     *               section of the Naming Concepts document
     * @param options options to pass to the implementation as described above, or {@code null}
     * @throws VException if the server object address could not be unmounted
     */
    void unmount(VContext context, String name, String server, Options options) throws VException;

    /**
     * A shortcut for {@link #unmount(VContext, String, String, Options, Callback)} with a {@code
     * null} options parameter.
     */
    void unmount(VContext context, String name, String server, Callback<Void> callback) throws
            VException;

    /**
     * Asynchronous version of {@link #unmount(VContext, String, String, Options)} that takes
     * in a callback whose {@code onSuccess} method will be called when the operation completes
     * successfully, and whose {@code onFailure} will be called if an error is encountered.
     */
    void unmount(VContext context, String name, String server, Options options, Callback<Void>
            callback) throws VException;

    /**
     * A shortcut for {@link #delete(VContext, String, boolean, Options)} with a {@code null}
     * options parameter.
     */
    void delete(VContext context, String name, boolean deleteSubtree) throws VException;

    /**
     * Deletes the name from a mount table. If the name has any children in its mount table, it (and
     * its children) will only be removed if {@code deleteSubtree} is true.
     * <p>
     * A particular implementation of this interface chooses which options to support,
     * but at the minimum it must handle the following pre-defined options:
     * <ul>
     *     <li>{@link io.v.v23.OptionDefs#SKIP_SERVER_ENDPOINT_AUTHORIZATION}</li>
     * </ul>
     *
     * @param context a client context
     * @param name the Vanadium name to delete, see also
     *             <a href="https://github.com/vanadium/docs/blob/master/glossary.md#object-name">the Name entry</a> in the
     *             glossary
     * @param deleteSubtree whether the entire tree rooted at {@code name} should be deleted
     * @param options options to pass to the implementation as described above, or {@code null}
     * @throws VException if the name could not be deleted
     */
    void delete(VContext context, String name, boolean deleteSubtree, Options options)
            throws VException;

    /**
     * A shortcut for {@link #delete(VContext, String, boolean, Options, Callback)} with a {@code
     * null} options parameter.
     */
    void delete(VContext context, String name, boolean deleteSubtree, Callback<Void> callback)
            throws VException;

    /**
     * Asynchronous version of {@link #delete(VContext, String, boolean, Options)} that takes
     * in a callback whose {@code onSuccess} method will be called when the operation completes
     * successfully, and whose {@code onFailure} will be called if an error is encountered.
     */
    void delete(VContext context, String name, boolean deleteSubtree, Options options,
                Callback<Void> callback) throws VException;

    /**
     * A shortcut for {@link #resolve(VContext, String, Options)} with a {@code null} options
     * parameter.
     */
    MountEntry resolve(VContext context, String name) throws VException;

    /**
     * Resolves the object name into its mounted servers.
     * <p>
     * A particular implementation of this interface chooses which options to support,
     * but at the minimum it must handle the following pre-defined options:
     * <ul>
     *     <li>{@link io.v.v23.OptionDefs#SKIP_SERVER_ENDPOINT_AUTHORIZATION}</li>
     * </ul>
     *
     * @param context a client context
     * @param name the Vanadium name to resolve, see also
     *             <a href="https://github.com/vanadium/docs/blob/master/glossary.md#object-name">the Name entry</a> in the
     *             glossary
     * @param options options to pass to the implementation as described above, or {@code null}
     * @return the {@link MountEntry} to which the name resolves, or {@code null} if it does not
     *         resolve
     * @throws VException if an error occurred during name resolution
     */
    MountEntry resolve(VContext context, String name, Options options) throws VException;

    /**
     * A shortcut for {@link #resolve(VContext, String, Options, Callback)} with a {@code null}
     * options parameter.
     */
    void resolve(VContext context, String name, Callback<MountEntry> callback) throws VException;

    /**
     * Asynchronous version of {@link #resolve(VContext, String, Options)} that takes in a
     * callback whose {@code onSuccess} method will be called when the operation completes
     * successfully, and whose {@code onFailure} will be called if an error is encountered.
     */
    void resolve(VContext context, String name, Options options, Callback<MountEntry> callback)
            throws VException;

    /**
     * A shortcut for {@link #resolve(VContext, String, Options)} with a {@code null} options
     * parameter.
     */
    MountEntry resolveToMountTable(VContext context, String name) throws VException;

    /**
     * Resolves the object name into the mounttables directly responsible for the name.
     * <p>
     * A particular implementation of this interface chooses which options to support,
     * but at the minimum it must handle the following pre-defined options:
     * <ul>
     *     <li>{@link io.v.v23.OptionDefs#SKIP_SERVER_ENDPOINT_AUTHORIZATION}</li>
     * </ul>
     *
     * @param context a client context
     * @param name the Vanadium name to resolve, see also
     *             <a href="https://github.com/vanadium/docs/blob/master/glossary.md#object-name">the Name entry</a> in the
     *             glossary
     * @param options options to pass to the implementation as described above, or {@code null}
     * @return the {@link MountEntry} of the mounttable server directly responsible for
     *         {@code name}, or {@code null} if {@code name} does not resolve
     * @throws VException if an error occurred during name resolution
     */
    MountEntry resolveToMountTable(VContext context, String name, Options options)
            throws VException;

    /**
     * A shortcut for {@link #resolve(VContext, String, Options, Callback)} with a {@code null}
     * options parameter.
     */
    void resolveToMountTable(VContext context, String name, Callback<MountEntry> callback) throws
            VException;

    /**
     * Asynchronous version of {@link #resolveToMountTable(VContext, String, Options)} that takes
     * in a callback whose {@code onSuccess} method will be called when the operation completes
     * successfully, and whose {@code onFailure} will be called if an error is encountered.
     */
    void resolveToMountTable(VContext context, String name, Options options, Callback<MountEntry>
            callback) throws VException;

    /**
     * Flushes resolution information cached for the given name. If anything was flushed it returns
     * {@code true}.
     *
     * @param context a client context
     * @param name a Vanadium name, see also <a href="https://github.com/vanadium/docs/blob/master/glossary.md#object-name">the
     *             Name entry</a> in the glossary
     * @return {@code true} if resolution information was for the name was flushed
     */
    boolean flushCacheEntry(VContext context, String name);

    /**
     * A shortcut for {@link #glob(VContext, String, Options)} with a {@code null} options
     * parameter.
     */
    Iterable<GlobReply> glob(VContext context, String pattern) throws VException;

    /**
     * Returns the iterator over all names matching the provided pattern. Note that due to the
     * inherently asynchronous nature of Vanadium's glob API, you should assume that calls to
     * the returned iterator's {@code next} method may block.
     * <p>
     * You should be aware that the iterator:
     * <p><ul>
     *     <li>can be created <strong>only</strong> once</li>
     *     <li>does not support {@link java.util.Iterator#remove remove}</li>
     * </ul>
     * <p>
     * A particular implementation of this interface chooses which options to support, but at the
     * minimum it must handle the following pre-defined options:
     * <ul>
     *     <li>{@link io.v.v23.OptionDefs#SKIP_SERVER_ENDPOINT_AUTHORIZATION}</li>
     * </ul>
     *
     * @param context a client context
     * @param pattern a pattern that should be matched
     * @param options options to pass to the implementation as described above, or {@code null}
     * @return        an iterator over {@link GlobReply} objects matching the provided pattern
     * @throws VException if an error is encountered
     */
    Iterable<GlobReply> glob(VContext context, String pattern, Options options)
            throws VException;

    /**
     * A shortcut for {@link #glob(VContext, String, Options, Callback)} with a {@code null} options
     * parameter.
     */
    void glob(VContext context, String pattern, Callback<Iterable<GlobReply>> callback)
            throws VException;

    /**
     * Asynchronously returns the iterator over all names matching the provided pattern. This
     * function returns immediately and the given non-{@code null} callback is called when the
     * operation completes (either successfully or with a failure). Generally, the callback will
     * be called when at least one entry can be read from the iterator. Subsequent calls to
     * {@code next} may block. You should not use the iterator on threads that should not block.
     * <p>
     * You should be aware that the iterator:
     * <p><ul>
     *     <li>can be created <strong>only</strong> once</li>
     *     <li>does not support {@link java.util.Iterator#remove remove}</li>
     * </ul>
     * <p>
     * A particular implementation of this interface chooses which options to support, but at the
     * minimum it must handle the following pre-defined options:
     * <ul>
     *     <li>{@link io.v.v23.OptionDefs#SKIP_SERVER_ENDPOINT_AUTHORIZATION}</li>
     * </ul>
     *
     * @param context a client context
     * @param pattern a pattern that should be matched
     * @param options options to pass to the implementation as described above, or {@code null}
     * @param callback a callback whose {@code onSuccess} method will be passed the {@link Iterable}
     *                 over {@link GlobReply} objects matching the provided pattern
     * @throws VException if an error is encountered
     */
    void glob(VContext context, String pattern, Options options, Callback<Iterable<GlobReply>>
            callback) throws VException;

    /**
     * Sets the roots that the local namespace is relative to.
     * <p>
     * All relative names passed to the other methods in this class will be interpreted as
     * relative to these roots. The roots will be tried in the order that they are specified in
     * {@code roots} list. Calling this method with an empty list will clear the currently
     * configured set of roots.
     *
     * @param roots the roots that will be used to turn relative paths into absolute paths, or
     *              {@code null} to clear the currently configured set of roots. Each entry should
     *              be a Vanadium name, see also <a href="https://github.com/vanadium/docs/blob/master/glossary.md#object-name">
     *              the Name entry</a> in the glossary
     */
    void setRoots(List<String> roots) throws VException;

    /**
     * A shortcut for {@link #setPermissions(VContext, String, Permissions, String, Options)} with a
     * {@code null} options parameter.
     */
    void setPermissions(VContext context, String name, Permissions permissions, String version)
            throws VException;

    /**
     * Sets the Permissions in a node in a mount table. If the caller tries to set a permission that
     * removes them from {@link io.v.v23.security.access.Constants#ADMIN}, the caller's original
     * admin blessings will be retained.
     * <p>
     * A particular implementation of this interface chooses which options to support,
     * but at the minimum it must handle the following pre-defined options:
     * <ul>
     *     <li>{@link io.v.v23.OptionDefs#SKIP_SERVER_ENDPOINT_AUTHORIZATION}</li>
     * </ul>
     *
     * @param context a client context
     * @param name the name of the node receiving the new permissions
     * @param permissions the permissions to set on the node
     * @param version a string containing the current permissions version number (e.g. {@code "4"}.
     *                If this value does not match the version known to the receiving server,
     *                a {@link VException} is thrown indicating that this call had no effect. If the
     *                version number is not specified, no version check is performed
     * @param options options to pass to the implementation as described above, or {@code null}
     * @throws VException if an error is encountered (e.g. if the caller does not have permission to
     *                    change the permissions)
     */
    void setPermissions(VContext context, String name, Permissions permissions, String version,
                        Options options) throws VException;

    /**
     * A shortcut for {@link #setPermissions(VContext, String, Permissions, String, Options,
     * Callback)} with a {@code null} options parameter.
     */
    void setPermissions(VContext context, String name, Permissions permissions, String version,
                        Callback<Void> callback) throws VException;

    /**
     * Asynchronous version of {@link #setPermissions(VContext, String, Permissions, String,
     * Options)} that takes in a callback whose {@code onSuccess} method will be called when the
     * operation completes successfully, and whose {@code onFailure} will be called if an error is
     * encountered.
     */
    void setPermissions(VContext context, String name, Permissions permissions, String version,
                        Options options, Callback<Void> callback) throws VException;

    /**
     * A shortcut for {@link #getPermissions(VContext, String, Options)} with a {@code null} options
     * parameter.
     */
    Map<String, Permissions> getPermissions(VContext context, String name) throws VException;

    /**
     * Returns the Permissions in a node in a mount table. The returned map will contain a single
     * entry whose key is the permissions version (see {@link #setPermissions(VContext, String,
     * Permissions, String, Options)}) and whose value is the permissions corresponding to that
     * version.
     * <p>
     * A particular implementation of this interface chooses which options to support,
     * but at the minimum it must handle the following pre-defined options:
     * <ul>
     *     <li>{@link io.v.v23.OptionDefs#SKIP_SERVER_ENDPOINT_AUTHORIZATION}</li>
     * </ul>
     *
     * @param context a client context
     * @param name the name of the node
     * @param options options to pass to the implementation as described above, or {@code null}
     * @return a single-entry map from permissions version to permissions for the named object
     * @throws VException if an error is encountered
     */
    Map<String, Permissions> getPermissions(VContext context, String name, Options options)
            throws VException;

    /**
     * A shortcut for {@link #getPermissions(VContext, String, Options, Callback)} with a
     * {@code null} options parameter.
     */
    void getPermissions(VContext context, String name, Callback<Map<String, Permissions>>
            callback) throws VException;

    /**
     * Asynchronous version of {@link #getPermissions(VContext, String, Options)} that takes in a
     * callback whose {@code onSuccess} method will be called when the operation completes
     * successfully, and whose {@code onFailure} will be called if an error is encountered.
     */
    void getPermissions(VContext context, String name, Options options, Callback<Map<String,
            Permissions>> callback) throws VException;
}
