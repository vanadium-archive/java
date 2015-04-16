// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.impl.google.rpc;

import io.v.v23.OutputChannel;
import io.v.v23.context.VContext;
import io.v.v23.naming.GlobReply;
import io.v.v23.rpc.Globber;
import io.v.v23.rpc.ServerCall;
import io.v.v23.rpc.StreamServerCall;
import io.v.v23.vdl.VdlValue;
import io.v.v23.vdl.VServer;
import io.v.v23.vdlroot.signature.Interface;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * VDLInvoker is a helper class that uses reflection to invoke VDL interface
 * methods for objects that implements those interfaces. It is required that the
 * provided objects implement exactly one VDL interface.
 */
public final class VDLInvoker {
    // A cache of ClassInfo objects, aiming to reduce the cost of expensive
    // reflection operations.
    private static Map<Class<?>, ClassInfo> serverWrapperClasses =
        new HashMap<Class<?>, ClassInfo>();

    private final static class ServerMethod {
        private final Object wrappedServer;
        private final Method method;
        private final VdlValue[] tags;

        public ServerMethod(Object wrappedServer, Method method, VdlValue[] tags) {
            this.wrappedServer = wrappedServer;
            this.method = method;
            this.tags = tags != null ? tags : new VdlValue[0];
        }

        public VdlValue[] getTags() { return this.tags; }

        public Object invoke(Object... args) throws IllegalAccessException,
                IllegalArgumentException, InvocationTargetException {
            return method.invoke(wrappedServer, args);
        }
    }

    private final Map<String, ServerMethod> invokableMethods = new HashMap<String, ServerMethod>();

    private final Map<Class<?>, ServerMethod> signatureMethods
            = new HashMap<Class<?>, ServerMethod>();

    private final Object server;

    /**
     * Creates a new invoker for the given object.
     *
     * @param  obj             server object we're invoking methods on
     * @throws VException      if the provided object is invalid (either null or doesn't implement
     *                         exactly one VDL interface)
     */
    public VDLInvoker(Object obj) throws VException {
        if (obj == null) {
            throw new VException("Can't create VDLInvoker with a null object.");
        }
        this.server = obj;

        List<Object> serverWrappers = wrapServer(obj);
        for (Object wrapper : serverWrappers) {
            final Class<?> c = wrapper.getClass();
            ClassInfo cInfo;
            synchronized (VDLInvoker.this) {
                cInfo = VDLInvoker.serverWrapperClasses.get(c);
            }
            if (cInfo == null) {
                cInfo = new ClassInfo(c);

                // Note that multiple threads might decide to create a new
                // ClassInfo and insert it
                // into the cache, but that's just wasted work and not a race
                // condition.
                synchronized (VDLInvoker.this) {
                    VDLInvoker.serverWrapperClasses.put(c, cInfo);
                }
            }

            final Map<String, Method> methods = cInfo.getMethods();
            final Method tagGetter = methods.get("getMethodTags");
            if (tagGetter == null) {
                throw new VException(String.format(
                    "Server class %s doesn't have the 'getMethodTags' method.",
                    c.getCanonicalName()));
            }

            final Method signatureMethod = methods.get("signature");
            if (signatureMethod == null) {
                throw new VException(String.format(
                    "Server class %s doesn't have the 'signature' method.",
                    c.getCanonicalName()));
            }
            signatureMethods.put(c, new ServerMethod(wrapper, signatureMethod, new VdlValue[] {}));

            for (Entry<String, Method> m : methods.entrySet()) {
                // Get the method tags.
                VdlValue[] tags = null;
                try {
                    tags = (VdlValue[])tagGetter.invoke(wrapper, null, m.getValue().getName());
                } catch (IllegalAccessException e) {
                    // getMethodTags() not defined.
                } catch (InvocationTargetException e) {
                    // getMethodTags() threw an exception.
                    throw new VException(String.format("Error getting tag for method %s: %s",
                        m.getKey(), e.getTargetException().getMessage()));
                }
                invokableMethods.put(m.getKey(), new ServerMethod(wrapper, m.getValue(), tags));
            }
        }
    }

    /**
     * Returns all the tags associated with the provided method or an empty array if
     * no tags have been associated with it.
     *
     * @param  method     method we are retrieving tags for.
     * @return            tags associated with the provided method.
     * @throws VException if the method doesn't exist.
     */
    public VdlValue[] getMethodTags(String method) throws VException {
        final ServerMethod m = this.invokableMethods.get(method);
        if (m == null) {
            throw new VException(String.format(
                    "Couldn't find method \"%s\" in class %s",
                    method, server.getClass().getCanonicalName()));
        }
        return m.getTags();
    }

    public Interface[] getSignature() throws VException {
        List<Interface> interfaces = new ArrayList<Interface>();

        for (Map.Entry<Class<?>, ServerMethod> entry : signatureMethods.entrySet()) {
            try {
                interfaces.add((Interface) entry.getValue().invoke((Object) null, (Object) null));
            } catch (IllegalAccessException e) {
                throw new VException(String.format(
                        "Could not invoke signature method for server class %s: %s",
                        server.getClass().getName(), e.toString()));
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                throw new VException(String.format(
                        "Could not invoke signature method for server class %s: %s",
                        server.getClass().getName(), e.toString()));
            }
        }
        return interfaces.toArray(new Interface[interfaces.size()]);
    }

    public io.v.v23.vdlroot.signature.Method getMethodSignature(String methodName)
            throws VException {
        Interface[] interfaces = getSignature();
        for (Interface iface : interfaces) {
            for (io.v.v23.vdlroot.signature.Method method : iface.getMethods()) {
                if (method.getName().equals(methodName)) {
                    return method;
                }
            }
        }

        throw new VException(String.format("Could not find method %s", methodName));
    }

    /**
     * Iterate through the veyron servers an object implements and generates
     * server wrappers for each.
     *
     * @param srv         the server object
     * @return            a list of server wrappers
     * @throws VException if the input server is invalid.
     */
    private List<Object> wrapServer(Object srv) throws VException {
        List<Object> stubs = new ArrayList<Object>();
        for (Class<?> iface : srv.getClass().getInterfaces()) {
            final VServer vs = iface.getAnnotation(VServer.class);
            if (vs == null) {
                continue;
            }
            // There should only be one constructor.
            if (vs.serverWrapper().getConstructors().length != 1) {
                throw new RuntimeException(
                        "Expected ServerWrapper to only have a single constructor");
            }
            final Constructor<?> constructor = vs.serverWrapper().getConstructors()[0];

            try {
                stubs.add(constructor.newInstance(srv));
            } catch (InstantiationException e) {
                throw new RuntimeException("Invalid constructor. Problem instanciating.", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Invalid constructor. Illegal access.", e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException("Invalid constructor. Problem invoking.", e);
            }
        }
        if (stubs.size() == 0) {
            throw new VException(
                    "Object does not implement a valid generated server interface.");
        }
        return stubs;
    }

    /**
     * InvokeReply stores the replies for the {@link #invoke} method. The
     * replies are VOM-encoded. In addition to replies, this class also stores
     * VOM-encoded application error, if any.
     */
    public static class InvokeReply {
        public byte[][] results = null; // can be null, e.g., if an error occurred.
        public byte[] vomAppError = null;
    }

    /**
     * VOM-decodes the provided arguments and invokes the given method using reflection.
     * VOM-encodes the reply. Application errors are returned along with the reply, while any other
     * encountered errors are thrown as exceptions.
     *
     * @param  context                  the context to pass to the invoked method
     * @param  call                     in-flight call information
     * @param  method                   name of the method to be invoked
     * @param  vomArgs                  VOM-encoded arguments to the method
     * @return InvokeReply              VOM-encoded invocation reply and application errors
     * @throws VException               if the method couldn't be invoked
     */
    public InvokeReply invoke(VContext context, StreamServerCall call, String method,
                              byte[][] vomArgs) throws VException {
        final ServerMethod m = this.invokableMethods.get(method);
        if (m == null) {
            throw new VException(String.format("Couldn't find method %s in class %s",
                    method, server.getClass().getCanonicalName()));
        }

        // VOM-decode arguments.
        final Object[] args = prepareArgs(m, context, call, vomArgs);

        // Invoke the method and process results.
        Object result = null;
        VException appError = null;
        try {
            result = m.invoke(args);
        } catch (InvocationTargetException e) { // The underlying method threw an exception.
            if ((e.getCause() instanceof VException)) {
                appError = (VException) e.getTargetException();
            } else {
                // Dump the stack trace locally.
                e.getTargetException().printStackTrace();
                throw new VException(String.format(
                    "Remote invocations of java methods may only throw VException, but call " +
                    "to %s threw %s", method, e.getTargetException().getClass()));
            }
        } catch (IllegalAccessException e) {
            throw new VException(String.format("Couldn't invoke method %s: %s",
                    m.method.getName(), e.getMessage()));
        }
        return prepareReply(m, result, appError);
    }

    /**
     * Implements the glob call. If the server implements the {@link Globber} interface, its {@link
     * Globber#glob glob} method is called to service this request. Otherwise the response channel
     * is simply closed without returning any entries.
     *
     * @param call            in-flight call information
     * @param pattern         the glob pattern
     * @param responseChannel the channel to which the glob reply should be sent
     * @throws VException  if any error occurs in native Go code
     */
    public void glob(ServerCall call, String pattern, OutputChannel<GlobReply> responseChannel)
            throws VException {
        if (server instanceof Globber) {
            ((Globber) server).glob(call, pattern, responseChannel);
        } else {
            responseChannel.close();
        }
    }

    private static Object[] prepareArgs(ServerMethod m, VContext context, StreamServerCall call,
                                        byte[][] vomArgs)
            throws VException {
        // The first arguments are always the context and the call, so we add those.
        final int argsLength = vomArgs.length + 2;
        final Type[] types = m.method.getGenericParameterTypes();
        if (argsLength != types.length) {
            throw new VException(String.format(
                "Mismatch in number of arguments for method %s: want %d, have %d",
                m.method.getName(), types.length, argsLength));
        }

        final Object[] ret = new Object[argsLength];
        ret[0] = context;
        ret[1] = call;
        for (int i = 0; i < vomArgs.length; i++) {
            ret[i + 2] = VomUtil.decode(vomArgs[i], types[i + 2]);
        }
        return ret;
    }

    private static InvokeReply prepareReply(ServerMethod m, Object result, VException appError)
            throws VException {
        final InvokeReply reply = new InvokeReply();
        if (appError != null) {
            reply.vomAppError = VomUtil.encode(appError, VException.class);
        }
        if (m.method.getReturnType() == void.class) {
            reply.results = new byte[0][];
        } else if (m.method.getReturnType().getDeclaringClass() == m.method.getDeclaringClass()) {
            // The return type was declared in the server definition, so this method has multiple
            // out args.
            final Field[] fields = m.method.getReturnType().getFields();
            reply.results = new byte[fields.length][];
            for (int i = 0; i < fields.length; i++) {
                try {
                    final Object value = result != null ? fields[i].get(result) : null;
                    reply.results[i] = VomUtil.encode(value, fields[i].getGenericType());
                } catch (IllegalAccessException e) {
                    throw new VException("Couldn't get field: " + e.getMessage());
                }
            }
        } else {
            reply.results = new byte[1][];
            reply.results[0] = VomUtil.encode(result, m.method.getGenericReturnType());
        }
        return reply;
    }

    private static class ClassInfo {
        final Map<String, Method> methods = new HashMap<String, Method>();

        ClassInfo(Class<?> c) throws VException {
            final Method[] methodList = c.getDeclaredMethods();
            for (int i = 0; i < methodList.length; i++) {
                final Method method = methodList[i];
                Method oldval = null;
                try {
                    oldval = this.methods.put(method.getName(), method);
                } catch (IllegalArgumentException e) {
                } // method not an VDL method.
                if (oldval != null) {
                    throw new VException("Overloading of method " + method.getName()
                            + " not allowed on server wrapper");
                }
            }
        }

        Map<String, Method> getMethods() {
            return this.methods;
        }
    }
}
