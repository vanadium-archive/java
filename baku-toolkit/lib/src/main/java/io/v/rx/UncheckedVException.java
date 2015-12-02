// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx;

import com.google.common.collect.Iterables;

import java.util.Arrays;

import io.v.v23.verror.VException;

/**
 * This wrapper for {@link VException} facilitates its use with lambdas and RxJava. Where this is
 * used, it is expected that alternate error handling mechanisms are in place.
 */
public class UncheckedVException extends RuntimeException {
    public UncheckedVException(final VException cause) {
        super(cause);
    }

    @Override
    public VException getCause() {
        return (VException)super.getCause();
    }

    public boolean isIdIn(final Iterable<VException.IDAction> ids) {
        return Iterables.any(ids, id -> id.getID().equals(getCause().getID()));
    }

    public boolean isIdIn(final VException.IDAction... ids) {
        return isIdIn(Arrays.asList(ids));
    }

    public static boolean isIdIn(final Throwable t, final Iterable<VException.IDAction> ids) {
        return t instanceof UncheckedVException && ((UncheckedVException)t).isIdIn(ids);
    }

    public static boolean isIdIn(final Throwable t, final VException.IDAction... ids) {
        return isIdIn(t, Arrays.asList(ids));
    }
}
