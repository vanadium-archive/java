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
     * A key for an option of type {@link java.lang.Boolean} that if provided and {@code true}
     * causes clients to ignore the blessings in remote (server) endpoint during authorization.
     * With this option enabled, clients are susceptible to man-in-the-middle attacks where an
     * imposter server has taken over the network address of a real server.
     */
    public static final String SKIP_SERVER_ENDPOINT_AUTHORIZATION =
            "io.v.v23.SKIP_SERVER_ENDPOINT_AUTHORIZATION";
}