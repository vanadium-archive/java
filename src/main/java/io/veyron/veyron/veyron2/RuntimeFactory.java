package io.veyron.veyron.veyron2;

import android.content.Context;
import io.veyron.veyron.veyron2.VRuntime;

/**
 * RuntimeFactory creates new runtimes.  It represents an entry point into the Veyron codebase.
 * The expected usage pattern of this class goes something like this:
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
	/**
	 * Initializes the global instance of the runtime.  Calling this method multiple times
	 * will always return the result of the first call to {@code init()} (ignoring subsequently
	 * provided options). All Veyron apps should call {@code init()} as the first thing in their
	 * execution flow.
	 *
	 * @param ctx   android context.
	 * @param opts  runtime options.
	 * @return      a pre-initialized runtime instance.
	 */
	public static synchronized VRuntime initRuntime(Context ctx, Options opts) {
		return io.veyron.veyron.veyron.runtimes.google.VRuntime.initRuntime(ctx, opts);
	}

	/**
	 * Returns the global, pre-initialized instance of a runtime, i.e., the runtime instance
	 * returned by the first call to {@code init()}.  This method requires that {@code init()}
	 * has already been invoked.
	 *
	 * @return default runtime instance.
	 */
	public static synchronized VRuntime defaultRuntime() {
		return io.veyron.veyron.veyron.runtimes.google.VRuntime.defaultRuntime();
	}

	/**
	 * Creates and initializes a new runtime instance.  This method should be used in unit tests
	 * and any situation where a single global runtime instance is inappropriate.
	 *
	 * @param ctx   android context.
	 * @param opts  runtime options.
	 * @return      a new runtime instance.
	 */
	public static VRuntime newRuntime(Context ctx, Options opts) {
		return io.veyron.veyron.veyron.runtimes.google.VRuntime.newRuntime(ctx, opts);
	}
}