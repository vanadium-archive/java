// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.rpc;

import io.v.v23.OutputChannel;
import io.v.v23.context.VContext;
import io.v.v23.naming.GlobReply;
import io.v.v23.vdl.MultiReturn;
import io.v.v23.vdl.VServer;
import io.v.v23.vdl.VdlValue;
import io.v.v23.vdlroot.signature.Interface;
import io.v.v23.verror.VException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * An {@link Invoker} that uses reflection to make each compatible exported method in the provided
 * object available.
 *
 * The provided object must implement interface(s) whose methods satisfy the following constraints:
 * <p><ol>
 *     <li>The first in-arg must be a {@link VContext}.</li>
 *     <li>The second in-arg must be a {@link ServerCall}.</li>
 *     <li>For streaming methods, the third in-arg must be a {@link io.v.v23.vdl.Stream}.</li>
 *     <li>If the return value is a class annotated with
 *         {@link io.v.v23.vdl.MultiReturn @MultiReturn} annotation, the fields of that class are
 *         interpreted as multiple return values for that method; otherwise, return values are
 *         interpreted as-is.</li>
 *     <li>{@link VException} must be thrown on error.</li>
 * </ol>
 * <p>
 * In addition, the interface must have a corresponding wrapper object and point
 * to it via a {@link io.v.v23.vdl.VServer @VServer} annotation.  This wrapper object unifies the
 * streaming and non-streaming methods under the same set of constraints:
 * <p><ol>
 *     <li>The first in-arg must be {@link VContext}.
 *     <li>The second in-arg must be {@link StreamServerCall}.
 *     <li>{@link VException} is thrown on error.
 * </ol>
 * <p>
 * Each wrapper method should invoke the corresponding interface method.  In addition, a wrapper
 * must provide a constructor that takes the interface as an argument.
 * <p>
 * A wrapper may optionally implement the following methods:
 * <p><ul>
 *     <li>{@code signature()}, which returns the signatures of all server methods.</li>
 *     <li>{@code getMethodTags(method)}, which returns tags for the given method.</li>
 * </ul><p>
 * If a server implements {@link Globber} interface, its {@link Globber#glob glob} method will be
 * invoked on all {@link #glob glob} calls on the {@link Invoker}.
 * <p>
 * Here is an example implementation for the object, as well as the interface and the wrapper.
 * <p>
 * Object:
 * <p><blockquote><pre>
 * public class Server implements ServerInterface, Globber {
 *     public String notStreaming(VContext context, ServerCall call) throws VException { ... }
 *     public String streaming(VContext context, ServerCall call, Stream stream)
 *             throws VException { ... }
 *     public void glob(ServerCall call, String pattern, OutputChannel<GlobReply> response)
 *             throws VException { ... }
 * }</pre></blockquote><p>
 * Interface:
 * <p><blockquote><pre>
 * {@literal @}io.v.v23.vdl.VServer(
 *     serverWrapper = ServerWrapper.class
 * )
 * public interface ServerInterface {
 *     String notStreaming(VContext context, ServerCall call) throws VException;
 *     String streaming(VContext context, ServerCall call, Stream stream) throws VException;
 * }
 * </pre></blockquote><p>
 * Wrapper:
 * <p><blockquote><pre>
 * public class ServerWrapper {
 *     public ServerWrapper(ServerInterface server) { this.server = server; }
 *     public String notStreaming(VContext context, StreamServerCall call) throws VException {
 *         return this.server.notStreaming(context, call);
 *     }
 *     public String streaming(VContext context, StreamServerCall call) throws VException {
 *         // Generate vdl.Stream
 *         return this.server.streaming(context, call, stream);
 *
 *     public Interface signature() {
 *         // Generate signatures for methods streaming() and notStreaming().
 *         return signatures;
 *     }
 *     public VdlValue[] getMethodTags(String method) throws VException {
 *         if ("notStreaming".equals(method)) { return ... }
 *         if ("streaming".equals(method)) { return ... }
 *         throw new VException("Unrecognized method: " + method);
 *     }
 * }</pre></blockquote><p>
 * Typically, the interface and the wrapper will be provided by the vdl generator: users would
 * implement only the object above.
 */
public final class ReflectInvoker implements Invoker {
    // A cache of ClassInfo objects, aiming to reduce the cost of expensive
    // reflection operations.
    private static Map<Class<?>, ClassInfo> serverWrapperClasses =
        new HashMap<Class<?>, ClassInfo>();

    private final static class ServerMethod {
        private final Object wrappedServer;
        private final Method method;
        private final VdlValue[] tags;
        private final Type[] argTypes;
        private final Type[] resultTypes;

        public ServerMethod(Object wrappedServer, Method method, VdlValue[] tags)
                throws VException {
            this.wrappedServer = wrappedServer;
            this.method = method;
            this.tags = tags != null ? Arrays.copyOf(tags, tags.length) : new VdlValue[0];
            Type[] args = method.getGenericParameterTypes();
            this.argTypes = Arrays.copyOfRange(args, 2, args.length);
            Class<?> returnType = method.getReturnType();
            if (returnType == void.class) {
                this.resultTypes = new Type[0];
            } else if (returnType.getAnnotation(MultiReturn.class) != null) {
                // Multiple return values.
                Field[] fields = returnType.getFields();
                this.resultTypes = new Type[fields.length];
                for (int i = 0; i < fields.length; ++i) {
                    this.resultTypes[i] = fields[i].getGenericType();
                }
            } else {
                this.resultTypes = new Type[] { method.getGenericReturnType() };
            }
        }
        public Method getReflectMethod() {
            return this.method;
        }
        public VdlValue[] getTags() {
            return Arrays.copyOf(this.tags, this.tags.length);
        }
        public Type[] getArgumentTypes() {
            return Arrays.copyOf(this.argTypes, this.argTypes.length);
        }
        public Type[] getResultTypes() {
            return Arrays.copyOf(this.resultTypes, this.resultTypes.length);
        }
        public Object invoke(Object... args) throws IllegalAccessException,
                IllegalArgumentException, InvocationTargetException {
            return method.invoke(wrappedServer, args);
        }
    }

    private final Map<String, ServerMethod> invokableMethods = new HashMap<String, ServerMethod>();

    private final Map<Object, Method> signatureMethods = new HashMap<Object, Method>();

    private final Object server;

    /**
     * Creates a new {@link ReflectInvoker} object.
     *
     * @param  obj        object whose methods will be invoked
     * @throws VException if the {@link ReflectInvoker} couldn't be created
     */
    public ReflectInvoker(Object obj) throws VException {
        if (obj == null) {
            throw new VException("Can't create ReflectInvoker with a null object.");
        }
        this.server = obj;
        List<Object> serverWrappers = wrapServer(obj);
        for (Object wrapper : serverWrappers) {
            Class<?> c = wrapper.getClass();
            ClassInfo cInfo;
            synchronized (ReflectInvoker.this) {
                cInfo = ReflectInvoker.serverWrapperClasses.get(c);
            }
            if (cInfo == null) {
                cInfo = new ClassInfo(c);

                // Note that multiple threads might decide to create a new
                // ClassInfo and insert it
                // into the cache, but that's just wasted work and not a race
                // condition.
                synchronized (ReflectInvoker.this) {
                    ReflectInvoker.serverWrapperClasses.put(c, cInfo);
                }
            }

            Map<String, Method> methods = cInfo.getMethods();
            Method tagGetter = methods.get("getMethodTags");
            Method signatureMethod = methods.get("signature");
            if (signatureMethod != null) {
                signatureMethods.put(wrapper, signatureMethod);
            }

            for (Entry<String, Method> m : methods.entrySet()) {
                // Make sure that the method signature is correct.
                Type[] argTypes = m.getValue().getGenericParameterTypes();
                if (argTypes.length < 2 ||
                        argTypes[0] != VContext.class || argTypes[1] != StreamServerCall.class) {
                    continue;
                }
                // Get the method tags.
                VdlValue[] tags = null;
                if (tagGetter != null) {
                    try {
                        tags = (VdlValue[])tagGetter.invoke(wrapper, m.getValue().getName());
                    } catch (IllegalAccessException e) {
                        // getMethodTags() not defined.
                    } catch (InvocationTargetException e) {
                        // getMethodTags() threw an exception.
                        throw new VException(String.format("Error getting tag for method %s: %s",
                            m.getKey(), e.getTargetException().getMessage()));
                    }
                }
                invokableMethods.put(m.getKey(), new ServerMethod(wrapper, m.getValue(), tags));
            }
        }
    }

    @Override
    public Object[] invoke(VContext context, StreamServerCall call, String method, Object[] args)
            throws VException {
        ServerMethod m = findMethod(method);
        try {
            // Invoke the method and process results.
            Object[] allArgs = new Object[2 + args.length];
            allArgs[0] = context;
            allArgs[1] = call;
            System.arraycopy(args, 0, allArgs, 2, args.length);
            Object result = m.invoke(allArgs);
            return prepareReply(m, result);
        } catch (InvocationTargetException e) { // The underlying method threw an exception.
            if ((e.getCause() instanceof VException)) {
                throw (VException) e.getCause();
            } else {
                // Dump the stack trace locally.
                e.getTargetException().printStackTrace();
                throw new VException(String.format(
                    "Remote invocations of java methods may only throw VException, but call " +
                    "to %s threw %s", method, e.getTargetException().getClass()));
            }
        } catch (IllegalAccessException e) {
            throw new VException(
                String.format("Couldn't invoke method %s: %s", method, e.getMessage()));
        }
    }

    @Override
    public Interface[] getSignature(VContext ctx, ServerCall call) throws VException {
        List<Interface> interfaces = new ArrayList<Interface>();
        for (Map.Entry<Object, Method> entry : signatureMethods.entrySet()) {
            try {
                interfaces.add((Interface) entry.getValue().invoke(entry.getKey()));
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

    @Override
    public io.v.v23.vdlroot.signature.Method getMethodSignature(
            VContext ctx, ServerCall call, String methodName) throws VException {
        Interface[] interfaces = getSignature(ctx, call);
        for (Interface iface : interfaces) {
            for (io.v.v23.vdlroot.signature.Method method : iface.getMethods()) {
                if (method.getName().equals(methodName)) {
                    return method;
                }
            }
        }
        throw new VException(String.format("Could not find method %s", methodName));
    }

    @Override
    public Type[] getArgumentTypes(String method) throws VException {
        return findMethod(method).getArgumentTypes();
    }

    @Override
    public Type[] getResultTypes(String method) throws VException {
        return findMethod(method).getResultTypes();
    }

    @Override
    public VdlValue[] getMethodTags(String method) throws VException {
        return findMethod(method).getTags();
    }

    @Override
    public void glob(ServerCall call, String pattern, OutputChannel<GlobReply> responseChannel)
            throws VException {
        if (server instanceof Globber) {
            ((Globber) server).glob(call, pattern, responseChannel);
        } else {
            responseChannel.close();
        }
    }

    private ServerMethod findMethod(String method) throws VException {
        ServerMethod m = this.invokableMethods.get(method);
        if (m == null) {
            throw new VException(String.format("Couldn't find method \"%s\" in class %s",
                    method, server.getClass().getCanonicalName()));
        }
        return m;
    }

    private static Object[] prepareReply(ServerMethod m, Object result) throws VException {
        Class<?> returnType = m.getReflectMethod().getReturnType();
        if (returnType == void.class) {
            return new Object[0];
        }
        if (returnType.getAnnotation(MultiReturn.class) != null) {
            // Multiple return values.
            Field[] fields = returnType.getFields();
            Object[] reply = new Object[fields.length];
            for (int i = 0; i < fields.length; i++) {
                try {
                    reply[i] = result != null ? fields[i].get(result) : null;
                } catch (IllegalAccessException e) {
                    throw new VException("Couldn't get field: " + e.getMessage());
                }
            }
            return reply;
        }
        return new Object[] { result };
    }

    /**
     * Iterates through the Vanadium servers the object implements and generates
     * server wrappers for each.
     */
    private List<Object> wrapServer(Object srv) throws VException {
        List<Object> stubs = new ArrayList<Object>();
        for (Class<?> iface : srv.getClass().getInterfaces()) {
            VServer vs = iface.getAnnotation(VServer.class);
            if (vs == null) {
                continue;
            }
            // There should only be one constructor.
            if (vs.serverWrapper().getConstructors().length != 1) {
                throw new RuntimeException(
                        "Expected ServerWrapper to only have a single constructor");
            }
            Constructor<?> constructor = vs.serverWrapper().getConstructors()[0];

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

    private static class ClassInfo {
        final Map<String, Method> methods = new HashMap<String, Method>();

        ClassInfo(Class<?> c) throws VException {
            Method[] methodList = c.getDeclaredMethods();
            for (int i = 0; i < methodList.length; i++) {
                Method method = methodList[i];
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
