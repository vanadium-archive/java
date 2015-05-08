// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Instances of this exception indicate that an error has occurred while loading the Vanadium native
 * shared library.
 */
public class VLoaderException extends RuntimeException {
    private final List<Throwable> exceptions;

    VLoaderException(List<Throwable> exceptions) {
        this.exceptions = ImmutableList.copyOf(exceptions);
    }

    /**
     * Returns the list of exceptions that were encountered when trying to load the native
     * libraries.
     */
    public List<Throwable> getExceptions() {
        return exceptions;
    }
}
