package com.veyron2.runtime;

import com.veyron2.ipc.Runtime;

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
  private static boolean initialized;

  /**
   * Initialize the runtime factory, creating a pre-initialized instance of a Runtime.
   * Invoking this operation is optional (Runtime creation methods below ensure that it will
   * be invoked, exactly once), but as it can be expensive it is provided for performance-aware
   * clients to invoke at an opportune time (e.g., initialization).
   *
   * @return Runtime a pre-initialized runtime instance.
   */
  public static synchronized Runtime init() {
    if (!initialized) {
      initialized = true;
      runtime = new com.veyron.runtimes.google.ipc.Runtime();
    }
    return runtime;
  }
  /**
   * Returns the pre-initialized instance of a Runtime.
   *
   * @return Runtime a pre-initialized runtime instance.
   */
  public static synchronized Runtime getRuntime() {
    init();
    return runtime;
  }

  /**
   * Creates and returns a new Runtime instance.
   *
   * @return Runtime a new Runtime instance.
   */
  public static Runtime newRuntime() {
    init();
    return new com.veyron.runtimes.google.ipc.Runtime();
  }

}