// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.v.v23.OptionDefs;
import io.v.v23.Options;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Callback;
import io.v.v23.rpc.Client;
import io.v.v23.rpc.ClientCall;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

import java.lang.reflect.Type;

/**
 * An implementation of the {@link Client} interface that calls to native code for most of its
 * functionalities.
 */
public class ClientImpl implements Client {
    private final long nativePtr;

    private native ClientCall nativeStartCall(long nativePtr, VContext context,
                                              String name, String method, byte[][] vomArgs,
                                              boolean skipServerAuth)
            throws VException;

    private native void nativeStartCallAsync(long nativePtr, VContext context,
                                             String name, String method, byte[][] vomArgs,
                                             boolean skipServerAuth,
                                             Callback<ClientCall> callback)
            throws VException;
    private native void nativeClose(long nativePtr);
    private native void nativeFinalize(long nativePtr);

    private ClientImpl(long nativePtr) {
        this.nativePtr = nativePtr;
    }

    private boolean shouldSkipServerAuth(Options opts) {
        return !opts.has(OptionDefs.SKIP_SERVER_ENDPOINT_AUTHORIZATION)
                    ? false
                    : opts.get(OptionDefs.SKIP_SERVER_ENDPOINT_AUTHORIZATION, Boolean.class);
    }

    // Implement io.v.v23.rpc.Client.
    @Override
    public ListenableFuture<ClientCall> startCall(
            VContext context, String name, String method, Object[] args, Type[] argTypes) throws VException {
        return startCall(context, name, method, args, argTypes, null);
    }
    @Override
    public ListenableFuture<ClientCall> startCall(VContext context, String name, String method, Object[] args, Type[]
            argTypes, Options opts) throws VException {
        final SettableFuture<ClientCall> future = SettableFuture.create();
        if (opts == null) {
            opts = new Options();
        }
        checkStartCallArgs(name, method, args, argTypes);
        Callback<ClientCall> callback = new Callback<ClientCall>() {
            @Override
            public void onSuccess(ClientCall result) {
                future.set(result);
            }

            @Override
            public void onFailure(VException error) {
                future.setException(error);
            }
        };
        nativeStartCallAsync(this.nativePtr, context, name, getMethodName(method),
            getEncodedVomArgs(args, argTypes), shouldSkipServerAuth(opts), callback);
        return future;
    }

    private String getMethodName(String method) {
        return Character.toUpperCase(method.charAt(0)) + method.substring(1);
    }

    /**
     * Asserts that the given parameters are valid for {@code startCall} and throws {@link
     * VException} if they are not.
     */
    private void checkStartCallArgs(String name, String method, Object[] args, Type[] argTypes)
            throws VException {
        if ("".equals(method)) {
            throw new VException(String.format("Empty method name invoked on object %s", name));
        }
        if (args.length != argTypes.length) {
            throw new VException(String.format(
                    "Argument count (%d) doesn't match type count (%d) for method %s of object %s",
                    args.length, argTypes.length, name, method));
        }
    }

    private byte[][] getEncodedVomArgs(Object[] args, Type[] argTypes) throws VException {
        // VOM-encode all input arguments, individually.
        byte[][] vomArgs = new byte[args.length][];
        for (int i = 0; i < args.length; ++i) {
            vomArgs[i] = VomUtil.encode(args[i], argTypes[i]);
        }
        return vomArgs;
    }

    @Override
    public void close() {
        nativeClose(this.nativePtr);
    }
    // Implement java.lang.Object.
    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (this.getClass() != other.getClass()) return false;
        return this.nativePtr == ((ClientImpl) other).nativePtr;
    }
    @Override
    public int hashCode() {
        return Long.valueOf(this.nativePtr).hashCode();
    }
    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr);
    }
}
