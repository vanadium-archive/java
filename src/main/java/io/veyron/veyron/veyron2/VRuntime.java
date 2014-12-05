package io.veyron.veyron.veyron2;

import go.Go;

import io.veyron.veyron.veyron2.ipc.Client;
import io.veyron.veyron.veyron2.context.Context;
import io.veyron.veyron.veyron2.ipc.Server;
import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.naming.Namespace;
import io.veyron.veyron.veyron2.security.Principal;

/**
 * VRuntime represents the local environment allowing clients and servers to communicate
 * with one another.  It contains a singleton runtime intance on which all methods
 * are invoked.
 *
 * The expected usage pattern of this class goes something like this:
 *
 *    ...
 *    VRuntime.init(opts);
 *    ...
 *    final Server s = VRuntime.newServer();
 *    ...
 *    final Client c = VRuntime.getClient();
 *    ...
 */
public class VRuntime {
	private static native void nativeInit();

	static {
		System.loadLibrary("jniwrapper");
		System.loadLibrary("veyronjni");
		Go.init();
		nativeInit();
	}

	private static volatile VRuntimeImpl runtime = null;

	/**
	 * Initializes the (singleton) runtime instance.  Calling this method multiple times will
	 * always return the result of the first call to {@code init()}, ignoring subsequently
	 * provided options.
	 *
	 * Invoking this method is optional; if not invoked, it will be auto-invoked with {@code null}
	 * options on a first call to another method.
	 *
	 * A caller may pass the following option that specifies the runtime implementation to be used:
	 *   {@code OptionDefs.RUNTIME}
	 *
	 * If this option isn't provided, the default runtime implementation is used.  The rest of
	 * the options are passed on to this runtime.  Currently, only the following options are
	 * recognized:
	 *   {@code OptionDefs.RUNTIME_PRINCIPAL}
	 *
	 * @param  opts runtime options.
	 */
	public static void init(Options opts) {
		if (runtime != null) return;
		synchronized (VRuntime.class) {
			if (runtime != null) return;
			if (opts == null) opts = new Options();
			// See if a runtime was provided as an option.
			if (opts.get(OptionDefs.RUNTIME) != null) {
				try {
					runtime = opts.get(OptionDefs.RUNTIME, VRuntimeImpl.class);
				} catch (VeyronException e) {
					throw new RuntimeException(String.format(
						"Option %s must be of type VRuntimeImpl", OptionDefs.RUNTIME));
				}
				return;
			}
			// Use the default runtime implementation.
			try {
				runtime = io.veyron.veyron.veyron.runtimes.google.VRuntimeImpl.create(opts);
			} catch (VeyronException e) {
	    		throw new RuntimeException(
	    			"Couldn't initialize Google Veyron Runtime: " + e.getMessage());
			}
		}
	}

	/**
	 * Creates a new client instance.
	 *
	 * @return                 the new client instance
	 * @throws VeyronException if a new client cannot be created
	 */
	public static Client newClient() throws VeyronException {
		return newClient(null);
	}

	/**
	 * Creates a new client instance with the provided options.  A particular runtime
	 * implementation chooses which options to support, but at the minimum must handle
	 * the following options:
	 *     CURRENTLY NO OPTIONS ARE MANDATED
	 *
	 * @param  opts            client options
	 * @return                 the new client instance
	 * @throws VeyronException if a new client cannot be created
	 */
	public static Client newClient(Options opts) throws VeyronException {
		return getImpl().newClient(opts);
	}

	/**
	 * Creates a new server instance.
	 *
	 * @return                 the new server instance
	 * @throws VeyronException if a new server cannot be created
	 */
	public static Server newServer() throws VeyronException {
		return newServer(null);
	}

	/**
	 * Creates a new server instance with the provided options.  A particular runtime
	 * implementation chooses which options to support, but at the minimum it must handle
	 * the following options:
	 *     CURRENTLY NO OPTIONS ARE MANDATED
	 *
	 * @param  opts            server options
	 * @return                 the new server instance
	 * @throws VeyronException if a new server cannot be created
	 */
	public static Server newServer(Options opts) throws VeyronException {
		return getImpl().newServer(opts);
	}

	/**
	 * Returns the pre-configured client that is created when the runtime is initialized.
	 *
	 * @return the pre-configured client instance.
	 */
	public static Client getClient() {
		return getImpl().getClient();
	}

	/**
	 * Returns a new context instance.
	 *
	 * @return context the new (client) context.
	 */
	public static Context newContext() {
		return getImpl().newContext();
	}

	/**
	 * Returns the principal that represents this runtime.
	 *
	 * @return the principal that represents this runtime.
	 */
	public static Principal getPrincipal() {
		return getImpl().getPrincipal();
	}

	/**
	 * Returns the pre-configured namespace that is created when the runtime is initialized.
	 *
	 * @return the pre-configured namespace instance.
	 */
	public static Namespace getNamespace() {
		return getImpl().getNamespace();
	}

	private static VRuntimeImpl getImpl() {
		init(null);
		return runtime;
	}
}
