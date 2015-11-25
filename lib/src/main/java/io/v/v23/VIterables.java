// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;

import java.util.Iterator;
import java.util.NoSuchElementException;

import io.v.v23.verror.VException;

/**
 * Contains static utility methods that operate on or return objects of type {@link VIterable}.
 */
public class VIterables {
    /**
     * Function used for transforming an input value into an output value.
     */
    public interface TransformFunction<F, T> {
        /**
         * Returns the result of transforming {@code from}.
         *
         * @throws VException if there was a transform error
         */
        T apply(F from) throws VException;
    }

    /**
     * Returns a {@link VIterable} that applies the provided {@code function} to each element
     * of {@code fromIterable}.
     */
    public static <F,T> VIterable<T> transform(VIterable<F> fromIterable,
                                               TransformFunction<? super F, ? extends T> function) {
        return new TransformedIterable<>(fromIterable, function);
    }

    private static class TransformedIterable<F, T> implements VIterable<T> {
        private final VIterable<F> fromIterable;
        private final TransformFunction<? super F, ? extends T> function;
        private boolean isCreated;
        private volatile VException error;

        private TransformedIterable(VIterable<F> fromIterable,
                                    TransformFunction<? super F, ? extends T> function) {
            this.fromIterable = fromIterable;
            this.function = function;
        }
        @Override
        public synchronized Iterator<T> iterator() {
            Preconditions.checkState(!isCreated, "Can only create one iterator.");
            isCreated = true;
            final Iterator<F> fromIterator = fromIterable.iterator();
            return new AbstractIterator<T>() {
                @Override
                protected T computeNext() {
                    try {
                        return function.apply(fromIterator.next());
                    } catch (NoSuchElementException e) {  // legitimate end of stream
                        return endOfData();
                    } catch (VException e) {
                        error = e;
                        return endOfData();
                    }
                }
            };
        }
        @Override
        public VException error() {
            if (fromIterable.error() != null) {
                return fromIterable.error();
            }
            return error;
        }
    }
}
