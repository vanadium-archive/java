// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23;

/**
 * OptionDefs defines commonly used options in the Veyron runtime.
 */
public class OptionDefs {
    /**
     * A key for an option of type {@link io.v.v23.VRuntime} that specifies a runtime
     * implementation.
     */
    public static String RUNTIME = "io.v.v23.RUNTIME";

    /**
     * A key for an option of type {@link io.v.v23.rpc.Client} that
     * specifies a client.
     */
    public static String CLIENT = "io.v.v23.CLIENT";
}