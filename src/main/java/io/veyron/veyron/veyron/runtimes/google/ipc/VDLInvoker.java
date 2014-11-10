package io.veyron.veyron.veyron.runtimes.google.ipc;

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
import io.veyron.veyron.veyron2.security.Label;
import io.veyron.veyron.veyron2.security.Security;
import io.veyron.veyron.veyron2.security.SecurityConstants;
import io.veyron.veyron.veyron2.ipc.ServerCall;
import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.vdl.VeyronServer;

/**
 * VDLInvoker is a helper class that uses reflection to invoke VDL interface
 * methods for objects that implements those interfaces. It is required that the
 * provided objects implement exactly one VDL interface.
 */
public final class VDLInvoker {
	private static final Label DEFAULT_LABEL = SecurityConstants.ADMIN_LABEL;

	// A cache of ClassInfo objects, aiming to reduce the cost of expensive
	// reflection operations.
	private static Map<Class<?>, ClassInfo> serverWrapperClasses =
		new HashMap<Class<?>, ClassInfo>();

	private final static class ServerMethod {
		private final Object wrappedServer;
		private final Method method;
		private final Object[] tags;

		public ServerMethod(Object wrappedServer, Method method, Object[] tags) {
			this.wrappedServer = wrappedServer;
			this.method = method;
			this.tags = tags;
		}

		public Object[] getTags() { return this.tags; }

		public Object invoke(Object... args) throws IllegalAccessException,
		IllegalArgumentException, InvocationTargetException {
			return method.invoke(wrappedServer, args);
		}
	}

	private final Map<String, ServerMethod> invokableMethods = new HashMap<String, ServerMethod>();

	private final Gson gson = new Gson();
	private final Class<?> serverClass; // Only used to make exception messages more clear.
	private String[] implementedServers;

	/**
	 * Returns a list of the servers that are implemented by the server object represented by
	 * the invoker. e.g. ["veyron2/service/proximity/ProximityScanner"]
	 *
	 * @return list of servers implemented by the server object represented by the invoker.
	 */
	public String[] getImplementedServers() {
		return this.implementedServers;
	}

	/**
	 * Creates a new invoker for the given object.
	 *
	 * @param obj                       server object we're invoking methods on
	 * @throws IllegalArgumentException if the provided object is invalid
	 *             (either null or doesn't implement exactly one VDL interface)
	 */
	// TODO(bprosnitz) We need to throw better exception types in the final
	// release.
	public VDLInvoker(Object obj) throws IllegalArgumentException, VeyronException {
		if (obj == null) {
			throw new IllegalArgumentException("Can't create VDLInvoker with a null object.");
		}
		this.serverClass = obj.getClass();

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
				throw new IllegalArgumentException(String.format(
					"Server class %s doesn't have the 'getMethodTags' method.",
					c.getCanonicalName()));
			}
			final Method signature = methods.get("signature");
			if (signature == null) {
				throw new IllegalArgumentException(String.format(
					"Server class %s doesn't have the 'signature' method.",
					c.getCanonicalName()));
			}
			for (Entry<String, Method> m : methods.entrySet()) {
				// Get the method tags.
				Object[] tags = null;
				try {
					tags = (Object[])tagGetter.invoke(wrapper, null, m.getValue().getName());
				} catch (IllegalAccessException e) {
					// getMethodTags() not defined.
				} catch (InvocationTargetException e) {
					// getMethodTags() threw an exception.
					throw new VeyronException(String.format("Error getting tag for method %q: %s",
						m.getKey(), e.getTargetException().getMessage()));
				}
				invokableMethods.put(m.getKey(), new ServerMethod(wrapper, m.getValue(), tags));
			}
		}
	}

	/**
	 * Returns all the tags associated with the provided method or {@code null} if no tags have
	 * been associated with it.
	 *
	 * @param  method                   method we are retrieving tags for.
	 * @return                          tags associated with the provided method.
	 * @throws IllegalArgumentException if the method doesn't exist.
	 */
	public Object[] getMethodTags(String method) throws IllegalArgumentException {
		final ServerMethod m = this.invokableMethods.get(method);
		if (m == null) {
			throw new IllegalArgumentException(String.format(
					"Couldn't find method %s in class %s",
					method, this.serverClass.getCanonicalName()));
		}
		return m.getTags();
	}

	/**
	 * Iterate through the veyron servers an object implements and generates
	 * server wrappers for each.
	 *
	 * @param srv                       the server object
	 * @return                          a list of server wrappers
	 * @throws IllegalArgumentException if the input server is invalid.
	 */
	private List<Object> wrapServer(Object srv) throws IllegalArgumentException {
		List<Object> stubs = new ArrayList<Object>();
		List<String> implementedServerList = new ArrayList<String>();
		for (Class<?> iface : srv.getClass().getInterfaces()) {
			final VeyronServer vs = iface.getAnnotation(VeyronServer.class);
			if (vs == null) {
				continue;
			}
			implementedServerList.add(vs.vdlPathName());
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
			throw new IllegalArgumentException(
					"Object does not implement a valid generated server interface.");
		}
		this.implementedServers = new String[implementedServerList.size()];
		implementedServerList.toArray(this.implementedServers);
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
		final ServerMethod m = this.invokableMethods.get(method);
		if (m == null) {
			throw new IllegalArgumentException(String.format(
					"Couldn't find method %s in class %s",
					method, this.serverClass.getCanonicalName()));
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

	private Object[] prepareArgs(ServerMethod m, ServerCall call, String[] inArgs)
			throws JsonSyntaxException {
		final Class<?>[] inTypes = m.method.getParameterTypes();
		assert inArgs.length == inTypes.length;

		// The first argument is always context, so we add it.
		final int argsLength = inArgs.length + 1;
		final Object[] ret = new Object[argsLength];
		ret[0] = call;
		for (int i = 0; i < inArgs.length; i++) {
			ret[i + 1] = this.gson.fromJson(inArgs[i], inTypes[i + 1]);
		}
		return ret;
	}

	private String[] prepareResults(ServerMethod m, Object result)
			throws IllegalArgumentException, IllegalAccessException {
		if (m.method.getReturnType() == void.class) {
			return new String[0];
		}
		if (m.method.getReturnType().getDeclaringClass() == m.method.getDeclaringClass()) {
			// The return type was declared in the server definition, so this
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
							+ " not allowed on server wrapper");
				}
			}
		}

		Map<String, Method> getMethods() {
			return this.methods;
		}
	}
}
