package com.veyron2;

import android.content.Context;
import com.veyron2.Runtime;
import com.veyron2.ipc.VeyronException;

/**
 * RuntimeFactory creates new Runtimes.  It represents an entry point into the Veyron codebase.
 * The expected usage pattern of this class goes something like this:
 *
 *    RuntimeFactory.init();
 *    ...
 *    final Runtime r = RuntimeFactory.getRuntime();
 *    final Server s = r.newServer();
 *    ...
 *    final Client c = r.getClient();
 *    ...
 */
public class RuntimeFactory {
  /**
   * Returns the initialized global instance of the Runtime.  Calling this method multiple times
   * will always return the result of the first call to <code>init</code> (ignoring subsequently
   * provided options). All Veyron apps should call <code>init</code> as the first thing in their
   * execution flow.
   *
   * @param ctx   android context.
   * @param opts  runtime options.
   * @return      a pre-initialized runtime instance.
   */
  public static synchronized Runtime init(Context ctx, Options opts) {
      return com.veyron.runtimes.google.Runtime.init(ctx, opts);
  }

  /**
   * Returns the global, pre-initialized instance of a Runtime.  Returns <code>null</code> if init()
   * hasn't already been invoked.
   *
   * @return a pre-initialized runtime instance.
   */
  public static synchronized Runtime defaultRuntime() {
    return com.veyron.runtimes.google.Runtime.defaultRuntime();
  }

  /**
   * Creates and initializes a new Runtime instance.  This method should be used in unit tests
   * and any situation where a single global runtime instance is inappropriate.
   *
   * @param ctx   android context.
   * @param opts  runtime options.
   * @return      a new Runtime instance.
   */
  public static Runtime newRuntime(Context ctx, Options opts) {
    return com.veyron.runtimes.google.Runtime.newRuntime(ctx, opts);
  }
}