package io.v.core.veyron.runtimes.google.ipc;

import io.v.core.veyron2.VeyronException;
import io.v.core.veyron2.util.VomUtil;
import io.v.core.veyron2.ipc.ServerCall;
import io.v.core.veyron2.vdl.VeyronServer;

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

	private final Class<?> serverClass; // Only used to make exception messages more clear.

	/**
	 * Creates a new invoker for the given object.
	 *
	 * @param  obj             server object we're invoking methods on
	 * @throws VeyronException if the provided object is invalid (either null or doesn't implement
	 *                         exactly one VDL interface)
	 */
	public VDLInvoker(Object obj) throws VeyronException {
		if (obj == null) {
			throw new VeyronException("Can't create VDLInvoker with a null object.");
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
				throw new VeyronException(String.format(
					"Server class %s doesn't have the 'getMethodTags' method.",
					c.getCanonicalName()));
			}
			final Method signature = methods.get("signature");
			if (signature == null) {
				throw new VeyronException(String.format(
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
	 * @throws VeyronException if the method doesn't exist.
	 */
	public Object[] getMethodTags(String method) throws VeyronException {
		final ServerMethod m = this.invokableMethods.get(method);
		if (m == null) {
			throw new VeyronException(String.format(
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
	 * @throws VeyronException if the input server is invalid.
	 */
	private List<Object> wrapServer(Object srv) throws VeyronException {
		List<Object> stubs = new ArrayList<Object>();
		for (Class<?> iface : srv.getClass().getInterfaces()) {
			final VeyronServer vs = iface.getAnnotation(VeyronServer.class);
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
			throw new VeyronException(
					"Object does not implement a valid generated server interface.");
		}
		return stubs;
	}

	/**
	 * InvokeReply stores the replies for the {@link #invoke} method. The
	 * replies are VOM-encoded. In addition to replies, this class also stores
	 * application error, if any.
	 */
	public static class InvokeReply {
		public byte[][] results = null; // can be null, e.g., if an error occurred.
		public boolean hasApplicationError = false;
		public String errorID = null;
		public String errorMsg = null;
	}

	/**
	 * VOM-decodes the provided arguments and invokes the given method using reflection.
	 * VOM-encodes the reply. Application errors are returned along with the reply, while any other
	 * encountered errors are thrown as exceptions.
	 *
	 * @param  method                   name of the method to be invoked
	 * @param  call                     in-flight call information
	 * @param  vomArgs                  VOM-encoded arguments to the method
	 * @return InvokeReply              VOM-encoded invocation reply and application errors
	 * @throws VeyronException          if the method couldn't be invoked
	 */
	public InvokeReply invoke(String method, ServerCall call, byte[][] vomArgs) throws VeyronException {
		final ServerMethod m = this.invokableMethods.get(method);
		if (m == null) {
			throw new VeyronException(String.format("Couldn't find method %s in class %s",
					method, this.serverClass.getCanonicalName()));
		}

		// VOM-decode arguments.
		final Object[] args = prepareArgs(m, call, vomArgs);

		// Invoke the method and process results.
		Object result = null;
		VeyronException appError = null;
		try {
			result = m.invoke(args);
		} catch (InvocationTargetException e) { // The underlying method threw an exception.
			if ((e.getCause() instanceof VeyronException)) {
				appError = (VeyronException) e.getTargetException();
			} else {
				// Dump the stack trace locally.
				e.getTargetException().printStackTrace();
				throw new VeyronException(String.format(
					"Remote invocations of java methods may only throw VeyronException, but call " +
					"to %s threw %s", method, e.getTargetException().getClass()));
			}
		} catch (IllegalAccessException e) {
		    throw new VeyronException(String.format("Couldn't invoke method %s: %s",
		            m.method.getName(), e.getMessage()));
		}
		return prepareReply(m, result, appError);
	}

	private static Object[] prepareArgs(ServerMethod m, ServerCall call, byte[][] vomArgs)
			throws VeyronException {
		// The first argument is always context, so we add it.
		final int argsLength = vomArgs.length + 1;
		final Type[] types = m.method.getGenericParameterTypes();
		if (argsLength != types.length) {
			throw new VeyronException(String.format(
				"Mismatch in number of arguments for method %s: want %d, have %d",
				m.method.getName(), types.length, argsLength));
		}

		final Object[] ret = new Object[argsLength];
		ret[0] = call;
		for (int i = 0; i < vomArgs.length; i++) {
			ret[i + 1] = VomUtil.decode(vomArgs[i], types[i + 1]);
		}
		return ret;
	}

	private static InvokeReply prepareReply(ServerMethod m, Object result, VeyronException appError)
			throws VeyronException {
		final InvokeReply reply = new InvokeReply();
		if (appError != null) {
			reply.hasApplicationError = true;
			reply.errorID = appError.getID();
			reply.errorMsg = appError.getMessage();
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
			        throw new VeyronException("Couldn't get field: " + e.getMessage());
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

		ClassInfo(Class<?> c) throws VeyronException {
			final Method[] methodList = c.getDeclaredMethods();
			for (int i = 0; i < methodList.length; i++) {
				final Method method = methodList[i];
				Method oldval = null;
				try {
					oldval = this.methods.put(method.getName(), method);
				} catch (IllegalArgumentException e) {
				} // method not an VDL method.
				if (oldval != null) {
					throw new VeyronException("Overloading of method " + method.getName()
							+ " not allowed on server wrapper");
				}
			}
		}

		Map<String, Method> getMethods() {
			return this.methods;
		}
	}
}
