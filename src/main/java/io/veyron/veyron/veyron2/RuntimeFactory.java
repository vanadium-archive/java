package io.veyron.veyron.veyron2;

import go.Go;
import io.veyron.veyron.veyron2.OptionDefs;
import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.VRuntime;
import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.security.Blessings;
import io.veyron.veyron.veyron2.security.ECDSASigner;
import io.veyron.veyron.veyron2.security.Principal;
import io.veyron.veyron.veyron2.security.Security;
import io.veyron.veyron.veyron2.security.SecurityConstants;
import io.veyron.veyron.veyron2.security.Signer;

import java.security.KeyStore;
import java.security.interfaces.ECPublicKey;

/**
 * RuntimeFactory creates new Java runtimes.  It represents an entry point into the Veyron
 * codebase.  The expected usage pattern of this class goes something like this:
 *
 *    RuntimeFactory.initRuntime(...);
 *    ...
 *    final Runtime r = RuntimeFactory.defaultRuntime();
 *    final Server s = r.newServer();
 *    ...
 *    final Client c = r.getClient();
 *    ...
 */
public class RuntimeFactory {
	private static native void nativeInit();

	static {
		System.loadLibrary("jniwrapper");
		System.loadLibrary("veyronjni");
		Go.init();
		nativeInit();
	}

	/**
	 * Initializes the global instance of the runtime.  Calling this method multiple times
	 * will always return the result of the first call to {@code init()} (ignoring subsequently
	 * provided options). All Veyron apps should call {@code init()} as the first thing in their
	 * execution flow.
	 *
	 * @param opts  runtime options.
	 * @return      a pre-initialized runtime instance.
	 */
	public static synchronized VRuntime initRuntime(Options opts) {
		if (opts == null) {
			opts = new Options();
		}
		return io.veyron.veyron.veyron.runtimes.google.VRuntime.initRuntime(opts);
	}

	/**
	 * Returns the global, pre-initialized instance of a runtime, i.e., the runtime instance
	 * returned by the first call to {@code init()}.  This method requires that {@code init()}
	 * has already been invoked.
	 *
	 * @return default runtime instance.
	 */
	public static synchronized VRuntime defaultRuntime() throws VeyronException {
		final VRuntime r = io.veyron.veyron.veyron.runtimes.google.VRuntime.defaultRuntime();
		if (r == null) {
			throw new VeyronException("Must call RuntimeFactory:initRuntime() first.");
		}
		return r;
	}

	/**
	 * Creates and initializes a new runtime instance.  This method should be used in unit tests
	 * and any situation where a single global runtime instance is inappropriate.
	 *
	 * @param opts  runtime options.
	 * @return      a new runtime instance.
	 */
	public static VRuntime newRuntime(Options opts) throws VeyronException {
		if (opts == null) {
			opts = new Options();
		}
		return io.veyron.veyron.veyron.runtimes.google.VRuntime.newRuntime(opts);
	}
}