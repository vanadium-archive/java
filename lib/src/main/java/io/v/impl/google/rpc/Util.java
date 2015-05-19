// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.rpc;

import io.v.v23.context.VContext;
import io.v.v23.rpc.Dispatcher;
import io.v.v23.rpc.Invoker;
import io.v.v23.rpc.ServiceObjectWithAuthorizer;
import io.v.v23.rpc.StreamServerCall;
import io.v.v23.security.Authorizer;
import io.v.v23.vdl.VdlValue;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

import java.lang.reflect.Type;

/**
 * Util provides a set of helper functions that move the Java-related computation out of
 * the native code.  The overall goal is to reduce the number of native->Java calls, as each
 * such call is exceedingly expensive (upto 500 cycles).
 */
class Util {
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
    static byte[][] invoke(Invoker invoker, VContext ctx, StreamServerCall call,
            String method, byte[][] vomArgs) throws VException {
        Type[] argTypes = invoker.getArgumentTypes(method);
        if (argTypes.length != vomArgs.length) {
            throw new VException(String.format(
                    "Wrong number of args, want %d, got %d", argTypes.length, vomArgs.length));
        }
        Object[] args = new Object[argTypes.length];
        for (int i = 0; i < argTypes.length; ++i) {
            args[i] = VomUtil.decode(vomArgs[i], argTypes[i]);
        }
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
        return vomResults;
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
}