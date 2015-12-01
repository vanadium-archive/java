// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.v.v23.verror.EndOfFileException;
import io.v.v23.verror.VException;

import static io.v.v23.VFutures.sync;

/**
 * Contains static utility methods that operate on or return objects of type {@link InputChannel}.
 */
public class InputChannels {
    /**
     * Function used for transforming an input value into an output value.
     */
    public interface TransformFunction<F, T> {
        /**
         * Returns the result of transforming {@code from}, or {@code null} if the value should
         * be skipped.
         *
         * @throws VException if there was a transform error
         */
        T apply(F from) throws VException;
    }

    /**
     * Returns an {@link InputChannel} that applies the provided {@code function} to each element
     * of {@code fromChannel}.
     */
    public static <F,T> InputChannel<T> transform(
            InputChannel<F> fromChannel, TransformFunction<? super F, ? extends T> function) {
        return new TransformedChannel<>(fromChannel, function);
    }

    /**
     * Returns a new {@link ListenableFuture} whose result is the list of all elements received
     * from the provided {@link InputChannel}.
     */
    public static <T> ListenableFuture<List<T>> asList(InputChannel<T> channel) {
        return nextListFuture(channel, new ArrayList<T>());
    }

    private static <T> ListenableFuture<List<T>> nextListFuture(final InputChannel<T> channel,
                                                                   final List<T> list) {
        return Futures.withFallback(
                Futures.transform(channel.recv(), new AsyncFunction<T, List<T>>() {
                    @Override
                    public ListenableFuture<List<T>> apply(T input) throws Exception {
                        list.add(input);
                        return nextListFuture(channel, list);
                    }
                }),
                new FutureFallback<List<T>>() {
                    @Override
                    public ListenableFuture<List<T>> create(Throwable t) throws Exception {
                        if (t instanceof EndOfFileException) {
                            return Futures.immediateFuture(list);
                        }
                        return Futures.immediateFailedFuture(t);
                    }
                });
    }

    /**
     * Returns a new {@link ListenableFuture} whose result is available when the provided
     * {@link InputChannel} has exhausted all of its elements.
     */
    public static ListenableFuture<Void> asDone(final InputChannel<?> channel) {
        return nextDoneFuture(channel);
    }

    private static <T> ListenableFuture<Void> nextDoneFuture(final InputChannel<T> channel) {
        return Futures.withFallback(
                Futures.transform(channel.recv(), new AsyncFunction<T, Void>() {
                    @Override
                    public ListenableFuture<Void> apply(T input) throws Exception {
                        return nextDoneFuture(channel);
                    }
                }),
                new FutureFallback<Void>() {
                    @Override
                    public ListenableFuture<Void> create(Throwable t) throws Exception {
                        if (t instanceof EndOfFileException) {
                            return Futures.immediateFuture(null);
                        }
                        return Futures.immediateFailedFuture(t);
                    }
                });
    }

    /**
     * Returns a {@link VIterable} over all the elements in {@code channel}, blocking in every
     * iteration.
     * <p>
     * The returned iterator will terminate gracefully iff {@code channel}'s
     * {@link InputChannel#recv} call fails with a {@link io.v.v23.verror.EndOfFileException}.
     */
    public static <T> VIterable<T> asIterable(InputChannel<? extends T> channel) {
        return new ChannelIterable<>(channel);
    }

    private static class TransformedChannel<F, T> implements InputChannel<T> {
        private final InputChannel<F> fromChannel;
        private final TransformFunction<? super F, ? extends T> function;

        private TransformedChannel(InputChannel<F> fromChannel,
                                   TransformFunction<? super F, ? extends T> function) {
            this.fromChannel = fromChannel;
            this.function = function;
        }
        @Override
        public ListenableFuture<T> recv() {
            return Futures.transform(fromChannel.recv(), new AsyncFunction<F, T>() {
                @Override
                public ListenableFuture<T> apply(F input) throws Exception {
                    T output = function.apply(input);
                    if (output == null) {
                        return recv();
                    }
                    return Futures.immediateFuture(output);
                }
            });
        }
    }

    private static class ChannelIterable<T> implements VIterable<T> {
        private final InputChannel<? extends T> fromChannel;
        private boolean isCreated;
        private volatile VException error;

        private ChannelIterable(InputChannel<? extends T> fromChannel) {
            this.fromChannel = fromChannel;
        }

        public synchronized Iterator<T> iterator() {
            Preconditions.checkState(!isCreated, "Can only create one iterator.");
            isCreated = true;
            return new AbstractIterator<T>() {
                protected T computeNext() {
                    try {
                        T result = sync(fromChannel.recv());
                        return result;
                    } catch (EndOfFileException e) {
                        return endOfData();
                    } catch (VException e) {
                        error = e;
                        return endOfData();
                    }
                }
            };
        }

        public VException error() {
            return error != null ? error : null;
        }
    }
}
