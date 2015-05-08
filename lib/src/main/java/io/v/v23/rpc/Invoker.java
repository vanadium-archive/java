// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

import io.v.v23.context.VContext;
import io.v.v23.vdl.VdlValue;
import io.v.v23.vdlroot.signature.Interface;
import io.v.v23.vdlroot.signature.Method;
import io.v.v23.verror.VException;

import java.lang.reflect.Type;

/**
 * Invoker defines the interface used by the server for invoking methods on named objects.
 * Typically {@code ReflectInvoker(object)} is used, which makes all public methods on the given
 * object invokable.
 */
public interface Invoker extends Globber {
    /**
     * Invokes the given method with the provided arguments.  Returns the results of the invocation.
     *
     * @param  ctx        context of the call
     * @param  call       call
     * @param  method     invoked method
     * @param  args       method arguments
     * @return            results of the invocation
     * @throws VException if there was an error invoking the method
     */
    Object[] invoke(VContext ctx, StreamServerCall call, String method, Object[] args)
            throws VException;

    /**
     * Returns the signatures of the interfaces that the underlying object implements.
     *
     * @param  ctx        context of the call
     * @param  call       call
     * @throws VException if the signatures couldn't be generated
     */
    Interface[] getSignature(VContext ctx, ServerCall call) throws VException;

    /**
     * Returns the signature of the given method.
     *
     * @param  ctx        context of the call
     * @param  call       call
     * @param  method     method name
     * @throws VException if the method signature couldn't be generated
     */
    Method getMethodSignature(VContext ctx, ServerCall call, String method) throws VException;

    /**
     * Returns the argument types for the given method.
     *
     * @param  method     method name
     * @throws VException if the argument types couldn't be retrieved
     */
    Type[] getArgumentTypes(String method) throws VException;

    /**
     * Returns the result types for the given method.
     *
     * @param  method     method name
     * @throws VException if the result types couldn't be retrieved
     */
    Type[] getResultTypes(String method) throws VException;

    /**
     * Returns all the tags associated with the provided method or an empty array if no tags have
     * been associated with it.
     *
     * @param  method     method name
     * @throws VException if the method doesn't exist
     */
    VdlValue[] getMethodTags(String method) throws VException;
}