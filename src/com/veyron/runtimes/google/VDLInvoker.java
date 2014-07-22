
package com.veyron.runtimes.google;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.veyron2.security.Label;
import com.veyron2.security.Security;
import com.veyron2.security.VeyronConsts;
import com.veyron2.ipc.ServerCall;
import com.veyron2.ipc.VeyronException;
import com.veyron2.vdl.VeyronService;

/**
 * VDLInvoker is a helper class that uses reflection to invoke VDL interface
 * methods for objects that implements those interfaces. It is required that the
 * provided objects implement exactly one VDL interface.
 */
public final class VDLInvoker {
    private static final Label DEFAULT_LABEL = VeyronConsts.ADMIN_LABEL;

    // A cache of ClassInfo objects, aiming to reduce the cost of expensive
    // reflection operations.
    private static Map<Class<?>, ClassInfo> serviceWrapperClasses = new HashMap<Class<?>, ClassInfo>();

    private final static class ServiceMethod {
        private final Object wrappedService;
        private final Method method;
        private final Label label;

        public ServiceMethod(Object wrappedService, Method method, Label label) {
            this.wrappedService = wrappedService;
            this.method = method;
            this.label = label;
        }

        public Label getLabel() { return this.label; }

        public Object invoke(Object... args) throws IllegalAccessException,
        IllegalArgumentException, InvocationTargetException {
            return method.invoke(wrappedService, args);
        }
    }

    private final Map<String, ServiceMethod> invokableMethods = new HashMap<String, ServiceMethod>();

    private final Gson gson = new Gson();
    private final Class<?> serviceClass; // Only used to make exception messages
    // more clear.
    private String[] implementedServices;

    // getImplementedServices gets a list of the services that are implmented by
    // the service object represented by the invoker.
    // e.g. ["veyron2/service/proximity/ProximityScanner"]
    public String[] getImplementedServices() {
    	return implementedServices;
    }

    /**
     * Creates a new invoker for the given object.
     *
     * @param obj                       service object we're invoking methods on
     * @return                          new VDL invoker instance
     * @throws IllegalArgumentException if the provided object is invalid
     *             (either null or doesn't implement exactly one VDL interface)
     */
    // TODO(bprosnitz) We need to throw better exception types in the final
    // release.
    public VDLInvoker(Object obj) throws IllegalArgumentException {
        if (obj == null) {
            throw new IllegalArgumentException("Can't create VDLInvoker with a null object.");
        }
        this.serviceClass = obj.getClass();

        List<Object> serviceWrappers = wrapService(obj);
        for (Object wrapper : serviceWrappers) {
            final Class<?> c = wrapper.getClass();
            ClassInfo cInfo;
            synchronized (VDLInvoker.this) {
                cInfo = VDLInvoker.serviceWrapperClasses.get(c);
            }
            if (cInfo == null) {
                cInfo = new ClassInfo(c);

                // Note that multiple threads might decide to create a new
                // ClassInfo and insert it
                // into the cache, but that's just wasted work and not a race
                // condition.
                synchronized (VDLInvoker.this) {
                    VDLInvoker.serviceWrapperClasses.put(c, cInfo);
                }
            }

            final Map<String, Method> methods = cInfo.getMethods();
            final Method tagGetter = methods.get("getMethodTags");
            if (tagGetter == null) {
                throw new IllegalArgumentException(String.format(
                    "Service class %s doesn't have the 'getMethodTags' method.",
                    c.getCanonicalName()));
            }
            for (Entry<String, Method> m : methods.entrySet()) {
                // Get the method label.
                Label label = DEFAULT_LABEL;
                try {
                    final Object[] tags =
                        (Object[])tagGetter.invoke(wrapper, null, m.getValue().getName());
                    for (Object tag : tags) {
                        if (tag instanceof Label && Security.IsValidLabel((Label)tag)) {
                            label = (Label)tag;
                            break;
                        }
                    }
                } catch (IllegalAccessException|InvocationTargetException e) {
                    // Use the default label.
                }    
                invokableMethods.put(m.getKey(), new ServiceMethod(wrapper, m.getValue(), label));
            }
        }
    }

    public Label getSecurityLabel(String method) throws IllegalArgumentException {
        final ServiceMethod m = this.invokableMethods.get(method);
        if (m == null) {
            throw new IllegalArgumentException(String.format(
                    "Couldn't find method %s in class %s",
                    method, this.serviceClass.getCanonicalName()));
        }
        return m.getLabel();
    }

    private String serviceName(Class<?> serviceClass) {
    	String name = serviceClass.getName();
    	if (name.length() < 4 || !"com.".equals(name.substring(0, 4))) {
    		// TODO(bprosnitz) Should we change this with an annotation of the service path?
    		throw new RuntimeException("Class name expected to start with 'com.'");
    	}
    	return name.substring(4).replace('.', '/');
    }

    /**
     * Iterate through the veyron services an object implements and generates
     * service wrappers for each.
     *
     * @param srv                       the service object
     * @return                          a list of service wrappers
     * @throws IllegalArgumentException if the input service is invalid.
     */
    private List<Object> wrapService(Object srv) throws IllegalArgumentException {
        List<Object> stubs = new ArrayList<Object>();
        List<String> implementedServiceList = new ArrayList<String>();
        for (Class<?> iface : srv.getClass().getInterfaces()) {
            VeyronService vs = iface.getAnnotation(VeyronService.class);
            if (vs == null) {
            	continue;
            }
            implementedServiceList.add(serviceName(iface));
            // There should only be one constructor.
            if (vs.serviceWrapper().getConstructors().length != 1) {
                throw new RuntimeException(
                        "Expected ServiceWrapper to only have a single constructor");
            }
            Constructor<?> constructor = vs.serviceWrapper().getConstructors()[0];

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
            throw new IllegalArgumentException(
                    "Object does not implement a valid generated service interface.");
        }
        implementedServices = new String[implementedServiceList.size()];
        implementedServiceList.toArray(implementedServices);
        return stubs;
    }

    /**
     * InvokeReply stores the replies for the {@link #invoke} method. The
     * replies are JSON-encoded. In addition to replies, this class also stores
     * application error, if any.
     */
    public class InvokeReply {
        public String[] results; // can be null, e.g., if an error occurred.
        public boolean hasApplicationError = false;
        public String errorID;
        public String errorMsg;
    }

    /**
     * Converts the provided arguments from JSON and invokes the given method
     * using reflection. JSON-encodes the reply. Application errors are returned
     * along with the reply, while any other encountered errors are thrown as
     * exceptions.
     *
     * @param method name of the method to be invoked
     * @param call in-flight call information
     * @param inArgs JSON encoded arguments to the method
     * @return InvokeReply JSON-encoded invocation reply and application errors
     * @throws IllegalArgumentException if invalid arguments are passed
     * @throws IllegalAccessException if a runtime access error occurs
     */
    public InvokeReply invoke(String method, ServerCall call, String[] inArgs) throws
    IllegalArgumentException, IllegalAccessException {
        final ServiceMethod m = this.invokableMethods.get(method);
        if (m == null) {
            throw new IllegalArgumentException(String.format(
                    "Couldn't find method %s in class %s",
                    method, this.serviceClass.getCanonicalName()));
        }

        // Decode JSON arguments.
        final Object[] args = this.prepareArgs(m, call, inArgs);

        // Invoke the method and process results.
        final InvokeReply reply = new InvokeReply();
        try {
            final Object result = m.invoke(args);
            reply.results = this.prepareResults(m, result);
        } catch (InvocationTargetException e) { // The underlying method threw
            // an exception.
            VeyronException ve;
            if ((e.getCause() instanceof VeyronException)) {
                ve = (VeyronException) e.getTargetException();
            } else {
                // Dump the stack trace locally.
                e.getTargetException().printStackTrace();

                ve = new VeyronException(
                        String.format(
                                "Remote invocations of java methods may only throw VeyronException, but call to %s threw %s",
                                method, e.getTargetException().getClass()));
            }
            reply.hasApplicationError = true;
            reply.errorID = ve.getID();
            reply.errorMsg = ve.getMessage();
        }

        return reply;
    }

    private Object[] prepareArgs(ServiceMethod m, ServerCall call, String[] inArgs)
            throws JsonSyntaxException {
        final Class<?>[] inTypes = m.method.getParameterTypes();
        assert inArgs.length == inTypes.length;

        // The first argument is always context, so we add it.
        final int argsLength = inArgs.length + 1;
        final Object[] ret = new Object[argsLength];
        ret[0] = call;
        for (int i = 0; i < inArgs.length; i++) {
            ret[i + 1] = this.gson.fromJson(inArgs[i], inTypes[i]);
        }
        return ret;
    }

    private String[] prepareResults(ServiceMethod m, Object result)
            throws IllegalArgumentException, IllegalAccessException {
        if (m.method.getReturnType().getDeclaringClass() == m.method.getDeclaringClass()) {
            // The return type was declared in the service definition, so this
            // method has multiple out args.
            final Field[] fields = m.method.getReturnType().getFields();
            final String[] reply = new String[fields.length];
            for (int i = 0; i < fields.length; i++) {
                reply[i] = this.gson.toJson(fields[i].get(result));
            }
            return reply;
        }
        final String[] reply = new String[1];
        reply[0] = this.gson.toJson(result);
        return reply;
    }

    private static class ClassInfo {
        final Map<String, Method> methods = new HashMap<String, Method>();

        ClassInfo(Class<?> c) throws IllegalArgumentException {
            final Method[] methodList = c.getDeclaredMethods();
            for (int i = 0; i < methodList.length; i++) {
                final Method method = methodList[i];
                Method oldval = null;
                try {
                    oldval = this.methods.put(method.getName(), method);
                } catch (IllegalArgumentException e) {
                } // method not an VDL method.
                if (oldval != null) {
                    throw new IllegalArgumentException("Overloading of method " + method.getName()
                            + " not allowed on service wrapper");
                }
            }
        }

        Map<String, Method> getMethods() {
            return this.methods;
        }
    }
}
