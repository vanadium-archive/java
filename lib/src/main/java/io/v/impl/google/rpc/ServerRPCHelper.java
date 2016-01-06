// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.rpc;

import io.v.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.rpc.Callback;
import io.v.v23.rpc.Dispatcher;
import io.v.v23.rpc.Invoker;
import io.v.v23.rpc.ServiceObjectWithAuthorizer;
import io.v.v23.rpc.StreamServerCall;
import io.v.v23.security.Authorizer;
import io.v.v23.vdl.VdlValue;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

import java.lang.reflect.Type;
import java.util.concurrent.Executor;

/**
 * ServerRPCHelper provides a set of helper functions for RPC handling on the server side.
 */
class ServerRPCHelper {
    private static native long nativeGoInvoker(Object serviceObject) throws VException;
    private static native long nativeGoAuthorizer(Object authorizer) throws VException;

    // Helper function for getting tags from the provided invoker.
    static byte[][] getMethodTags(Invoker invoker, String method) throws VException {
        VdlValue[] tags = invoker.getMethodTags(method);
        byte[][] vomTags = new byte[tags.length][];
        for (int i = 0; i < tags.length; ++i) {
            vomTags[i] = VomUtil.encode(tags[i]);
        }
        return vomTags;
    }

    // Helper function for invoking a method on the provided invoker.
    static void invoke(final Invoker invoker, final VContext ctx, final StreamServerCall call,
            final String method, byte[][] vomArgs, final Callback callback) throws VException {
        Type[] argTypes = invoker.getArgumentTypes(method);
        if (argTypes.length != vomArgs.length) {
            throw new VException(String.format(
                    "Wrong number of args, want %d, got %d", argTypes.length, vomArgs.length));
        }
        final Object[] args = new Object[argTypes.length];
        for (int i = 0; i < argTypes.length; ++i) {
            args[i] = VomUtil.decode(vomArgs[i], argTypes[i]);
        }
        // We need to return control to the Go thread immediately, otherwise if the invoked method
        // blocks, it will block the Go thread which will prevent any go-routine from getting
        // scheduled on it.  So, we invoke the server method on an executor.
        Executor executor = V.getExecutor(ctx);
        if (executor == null) {
            throw new VException("NULL executor in context: did you derive this context from " +
                    "the context returned by V.init()?");
        }
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Object[] results = invoker.invoke(ctx, call, method, args);
                    Type[] resultTypes = invoker.getResultTypes(method);
                    if (resultTypes.length != results.length) {
                        throw new VException(String.format(
                                "Wrong number of results, want %d, got %d", resultTypes.length, results.length));
                    }
                    byte[][] vomResults = new byte[resultTypes.length][];
                    for (int i = 0; i < resultTypes.length; ++i) {
                        vomResults[i] = VomUtil.encode(results[i], resultTypes[i]);
                    }
                    callback.onSuccess(vomResults);
                } catch (VException e) {
                    callback.onFailure(e);
                }
            }
        });
    }

    // Helper function for invoking a lookup method on the provided dispatcher.
    // The return value is:
    //    (1) null, if the dispatcher doesn't handle the object with the given suffix, or
    //    (2) an array containing:
    //        - pointer to the appropriate Go invoker,
    //        - pointer to the appropriate Go authorizer.
    static long[] lookup(Dispatcher d, String suffix) throws VException {
        ServiceObjectWithAuthorizer result = d.lookup(suffix);
        if (result == null) {  // object not handled
            return null;
        }
        Object obj = result.getServiceObject();
        if (obj == null) {
            throw new VException("Null service object returned by Java's dispatcher");
        }
        Authorizer auth = result.getAuthorizer();
        return new long[] { nativeGoInvoker(obj), auth == null ? 0 : nativeGoAuthorizer(auth) };
    }

    private ServerRPCHelper() {}
}