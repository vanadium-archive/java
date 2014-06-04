package com.veyron2.runtime;

import com.veyron2.ipc.Runtime;
import com.veyron2.ipc.VeyronException;

/**
 * RuntimeFactory creates new Runtimes.  It represents an entry point into the Veyron environment.
 * The expected usage pattern of this class goes something like this:
 *
 *    RuntimeFactory.init();  // optional
 *    ...
 *    final Runtime r = RuntimeFactory.getRuntime();
 *    final Server s = r.newServer();
 *    ...
 *    final Client c = r.getClient();
 *    ...
 */
public class RuntimeFactory {
  private static Runtime runtime;

  /**
   * Initialize the runtime factory, creating a pre-initialized instance of a Runtime.
   * Invoking this operation is optional (Runtime creation methods below ensure that it will
   * be invoked, exactly once), but as it can be expensive it is provided for performance-aware
   * clients to invoke at an opportune time (e.g., initialization).
   *
   * @return Runtime a pre-initialized runtime instance.
   */
  public static synchronized Runtime init() {
    return com.veyron.runtimes.google.ipc.Runtime.global();
  }

  /**
   * Returns the pre-initialized instance of a Runtime.
   *
   * @return Runtime a pre-initialized runtime instance.
   */
  public static synchronized Runtime getRuntime() {
    return com.veyron.runtimes.google.ipc.Runtime.global();
  }

  /**
   * Creates and returns a new Runtime instance.
   *
   * @return Runtime a new Runtime instance.
   * @throws VeyronException if the new runtime cannot be created.
   */
  public static Runtime newRuntime() throws VeyronException {
    return new com.veyron.runtimes.google.ipc.Runtime();
  }
}