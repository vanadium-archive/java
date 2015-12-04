// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23;

/**
 * Commonly used options in the Vanadium runtime.
 */
public class OptionDefs {
    /**
     * A key for an option of type {@link io.v.v23.VRuntime} that specifies a runtime
     * implementation.
     */
    public static final String RUNTIME = "io.v.v23.RUNTIME";

    /**
     * A key for an option of type {@link io.v.v23.rpc.Client} that specifies a client.
     */
    public static final String CLIENT = "io.v.v23.CLIENT";

    /**
     * A key for an option of type {@link Boolean} that if provided and {@code true}
     * causes clients to ignore the blessings in remote (server) endpoint during authorization.
     * With this option enabled, clients are susceptible to man-in-the-middle attacks where an
     * imposter server has taken over the network address of a real server.
     */
    public static final String SKIP_SERVER_ENDPOINT_AUTHORIZATION =
            "io.v.v23.SKIP_SERVER_ENDPOINT_AUTHORIZATION";

    /**
     * A key for an option of type {@link java.util.concurrent.Executor} that specifies
     * a thread executor for invoking server methods asynchronously.  If not specified, a
     * {@link java.util.concurrent.Executors#newCachedThreadPool default} executor will be used.
     */
    public static final String SERVER_THREAD_EXECUTOR = "io.v.v23.THREAD_EXECUTOR";

    /**
     * A key for an option of type {@link String} that specifies the directory that should be
     * used for storing the log files.  If not present, logs will be written into the system's
     * temporary directory.
     */
    public static final String LOG_DIR = "io.v.v23.LOG_DIR";

    /**
     * A key for an option of type {@link Boolean} that specifies whether all logs should be
     * written to standard error instead of files.
     */
    public static final String LOG_TO_STDERR = "io.v.v23.LOG_TO_STDERR";

    /**
     * A key for an option of type {@link Integer} that specifies the level of verbosity for the
     * for the {@code V} logs in the vanadium code.
     */
    public static final String LOG_VLEVEL = "io.v.v23.LOG_VLEVEL";

    /**
     * A key for an option of type {@link String} that specifies the comma-separated list of
     * {@code pattern=N}, where pattern is a literal file name (minus the extension suffix) or
     * a glob pattern, and N is the level of verbosity for the {@code V} logs in the vanadium code.
     * For example:
     * <p><blockquote><pre>
     *     vsync*=5,VRuntime=2
     * </pre></blockquote><p>
     */
    public static final String LOG_VMODULE = "io.v.v23.LOG_VMODULE";
}