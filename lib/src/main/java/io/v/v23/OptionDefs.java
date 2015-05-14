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
    public static String RUNTIME = "io.v.v23.RUNTIME";

    /**
     * A key for an option of type {@link java.lang.Integer} that specifies the maximum number
     * of CPUs that the default runtime should use.
     */
    public static String RUNTIME_NUM_CPUS = "io.v.v23.NUM_CPUS";

    /**
     * A key for an option of type {@link io.v.v23.rpc.Client} that specifies a client.
     */
    public static String CLIENT = "io.v.v23.CLIENT";

    /**
     * A key for an option of type {@link java.lang.Boolean} that if provided and {@code true}
     * causes clients to ignore the blessings in remote (server) endpoint during authorization.
     * With this option enabled, clients are susceptible to man-in-the-middle attacks where an
     * imposter server has taken over the network address of a real server.
     */
    public static String SKIP_SERVER_ENDPOINT_AUTHORIZATION =
            "io.v.v23.SKIP_SERVER_ENDPOINT_AUTHORIZATION";
}