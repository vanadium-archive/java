// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.v.v23.verror.VException;

/**
 * Contains static utility methods that operate on or return objects of type {@link Future}.
 */
public class VFutures {
    /**
     * Waits for the computation embodied by the given {@code future} to complete and then
     * retrieves its result.
     * <p>
     * This method requires that {@code future}'s computation may only throw a {@link VException} -
     * any other exception type will result in a {@link RuntimeException}.
     * <p>
     * Likewise, if the {@code future}'s execution raises an {@link InterruptedException}, this
     * method will raise a {@link RuntimeException}.
     *
     * @param future the future being executed
     * @return the computed result of the future
     * @throws VException if the computation embodied by the future threw a {@link VException}
     * @throws RuntimeException if the computation embodied by the future threw an exception
     *                          other than {@link VException}, or if the future's executions raised
     *                          an {@link InterruptedException}
     */
    public static <T> T sync(Future<T> future) throws VException {
        try {
            return future.get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof VException) {
                throw (VException) e.getCause();
            } else if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException(
                    "Vanadium futures may only raise a VException or a RuntimeException.",
                    e.getCause());
        } catch (InterruptedException e) {
            throw new RuntimeException("Vanadium future may not raise an InterruptedException.", e);
        }
    }

    /**
     * Waits if necessary for at most the given time for the computation embodied by the given
     * {@code future} to complete and then retrieves its result, if available.
     * <p>
     * This method requires that {@code future}'s computation may only throw a {@link VException} -
     * any other exception type will result in a {@link RuntimeException}.
     * <p>
     * Likewise, if the {@code future}'s execution results in an {@link InterruptedException}, this
     * method will raise a {@link RuntimeException}.
     *
     * @param future the future being executed
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the computed result of the future
     * @throws VException if the computation embodied by the future threw a {@link VException}
     * @throws RuntimeException if the computation embodied by the future threw an exception
     *                          other than {@link VException}, or if the future's executions raised
     *                          an {@link InterruptedException}
     * @throws TimeoutException if the future didn't complete in the allotted time
     */
    public static <T> T sync(Future<T> future, long timeout, TimeUnit unit)
            throws VException, TimeoutException {
        try {
            return future.get(timeout, unit);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof VException) {
                throw (VException) e.getCause();
            } else if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw new RuntimeException(
                    "Vanadium futures may only raise a VException or a RuntimeException.",
                    e.getCause());
        } catch (InterruptedException e) {
            throw new RuntimeException("Vanadium future may not raise an InterruptedException.", e);
        }
    }
}
