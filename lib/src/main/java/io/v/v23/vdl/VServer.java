// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.vdl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation specifying the wrapper object for a (server) interface.  Used by
 * {@link io.v.v23.rpc.ReflectInvoker}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface VServer {
    /**
     * Returns the wrapper object for a (server) interface.
     */
    Class<?> serverWrapper();
}
