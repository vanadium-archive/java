// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.namespace;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import io.v.v23.VIterable;
import io.v.v23.rpc.Callback;
import org.joda.time.Duration;

import java.util.List;
import java.util.Map;

import io.v.v23.Options;
import io.v.v23.context.VContext;
import io.v.v23.namespace.Namespace;
import io.v.v23.naming.GlobReply;
import io.v.v23.naming.MountEntry;
import io.v.v23.security.access.Permissions;
import io.v.v23.verror.VException;

/**
 * An implementation of {@link Namespace} that calls to native code for most of its
 * functionalities.
 */
public class NamespaceImpl implements Namespace {
    private final long nativePtr;

    private static native void nativeGlob(long nativePtr, VContext context, String pattern,
                                          Options options, Callback<VIterable<GlobReply>> callback);
    private static native void nativeMount(long nativePtr, VContext context, String name,
                                           String server, Duration ttl, Options options,
                                           Callback<Void> callback);
    private static native void nativeUnmount(long nativePtr, VContext context, String name,
                                             String server, Options options,
                                             Callback<Void> callback);
    private static native void nativeDelete(long nativePtr, VContext context, String name,
                                            boolean deleteSubtree, Options options,
                                            Callback<Void> callback);
    private static native void nativeResolveToMountTable(long nativePtr, VContext context,
                                                         String name, Options options,
                                                         Callback<MountEntry> callback);
    private static native void nativeResolve(long nativePtr, VContext context, String name,
                                             Options options, Callback<MountEntry> callback);
    private static native boolean nativeFlushCacheEntry(long nativePtr, VContext context,
                                                        String name);
    private static native void nativeSetRoots(long nativePtr, List<String> roots) throws VException;
    private static native void nativeSetPermissions(long nativePtr, VContext context, String name,
                                                    Permissions permissions, String version,
                                                    Options options, Callback<Void> callback);
    private static native void nativeGetPermissions(long nativePtr, VContext context, String name,
                                                    Options options,
                                                    Callback<Map<String, Permissions>> callback);
    private native void nativeFinalize(long nativePtr);

    private NamespaceImpl(long nativePtr) {
        this.nativePtr = nativePtr;
    }

    @Override
    public ListenableFuture<Void> mount(VContext context, String name, String server,
                                        Duration ttl) {
        return mount(context, name, server, ttl, null);
    }

    @Override
    public ListenableFuture<Void> mount(VContext context, String name, String server, Duration ttl,
                                        Options options) {
        FutureCallback<Void> callback = new FutureCallback<>();
        nativeMount(nativePtr, context, name, server, ttl, options, callback);
        return callback.getFuture();
    }

    @Override
    public ListenableFuture<Void> unmount(VContext context, String name, String server) {
        return unmount(context, name, server, null);
    }

    @Override
    public ListenableFuture<Void> unmount(VContext context, String name, String server,
                                          Options options) {
        FutureCallback<Void> callback = new FutureCallback<>();
        nativeUnmount(nativePtr, context, name, server, options, callback);
        return callback.getFuture();
    }

    @Override
    public ListenableFuture<Void> delete(VContext context, String name, boolean deleteSubtree) {
        return delete(context, name, deleteSubtree, null);
    }

    @Override
    public ListenableFuture<Void> delete(VContext context, String name, boolean deleteSubtree,
                                         Options options) {
        FutureCallback<Void> callback = new FutureCallback<>();
        nativeDelete(nativePtr, context, name, deleteSubtree, options, callback);
        return callback.getFuture();
    }

    @Override
    public ListenableFuture<MountEntry> resolve(VContext context, String name) {
        return resolve(context, name, null);
    }

    @Override
    public ListenableFuture<MountEntry> resolve(VContext context, String name, Options options) {
        FutureCallback<MountEntry> callback = new FutureCallback<>();
        nativeResolve(nativePtr, context, name, options, callback);
        return callback.getFuture();
    }

    @Override
    public ListenableFuture<MountEntry> resolveToMountTable(VContext context, String name) {
        return resolveToMountTable(context, name, null);
    }

    @Override
    public ListenableFuture<MountEntry> resolveToMountTable(VContext context, String name,
                                                            Options options) {
        FutureCallback<MountEntry> callback = new FutureCallback<>();
        nativeResolveToMountTable(nativePtr, context, name, options, callback);
        return callback.getFuture();
    }

    @Override
    public boolean flushCacheEntry(VContext context, String name) {
        return nativeFlushCacheEntry(nativePtr, context, name);
    }

    @Override
    public ListenableFuture<VIterable<GlobReply>> glob(VContext context, String pattern) {
        return glob(context, pattern, null);
    }

    @Override
    public ListenableFuture<VIterable<GlobReply>> glob(VContext context, String pattern,
                                                       Options options) {
        FutureCallback<VIterable<GlobReply>> callback = new FutureCallback<>();
        nativeGlob(nativePtr, context, pattern, options, callback);
        return callback.getFuture();
    }

    @Override
    public void setRoots(List<String> roots) throws VException {
        nativeSetRoots(nativePtr, roots);
    }

    @Override
    public ListenableFuture<Void> setPermissions(VContext context, String name,
                                                 Permissions permissions, String version) {
        return setPermissions(context, name, permissions, version, null);
    }

    @Override
    public ListenableFuture<Void> setPermissions(VContext context, String name,
                                                 Permissions permissions, String version,
                                                 Options options) {
        FutureCallback<Void> callback = new FutureCallback<>();
        nativeSetPermissions(nativePtr, context, name, permissions, version, options, callback);
        return callback.getFuture();
    }

    @Override
    public ListenableFuture<Map<String, Permissions>> getPermissions(VContext context,
                                                                     String name) {
        return getPermissions(context, name, null);
    }

    @Override
    public ListenableFuture<Map<String, Permissions>> getPermissions(VContext context, String name,
                                                                     Options options) {
        final FutureCallback<Map<String, Permissions>> callback = new FutureCallback<>();
        nativeGetPermissions(nativePtr, context, name, options, callback);
        return callback.getFuture();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (this.getClass() != other.getClass()) {
            return false;
        }
        return this.nativePtr == ((NamespaceImpl) other).nativePtr;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(this.nativePtr).hashCode();
    }

    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr);
    }

    private static class FutureCallback<T> implements Callback<T> {
        private final SettableFuture<T> future = SettableFuture.create();
        public ListenableFuture<T> getFuture() {
            return future;
        }
        @Override
        public void onSuccess(T result) {
            future.set(result);
        }
        @Override
        public void onFailure(VException error) {
            future.setException(error);
        }
    }
}
