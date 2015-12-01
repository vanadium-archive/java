// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

import io.v.v23.verror.EndOfFileException;
import io.v.v23.verror.VException;

import static com.google.common.truth.Truth.assertThat;
import static io.v.v23.VFutures.sync;

/**
 * Tests for static methods in {@link InputChannels} class.
 */
public class InputChannelsTest extends TestCase {
    @Override
    protected void setUp() throws Exception {
        V.init();
    }

    public void testTransform() throws Exception {
        {
            InputChannel<Long> chan = InputChannels.transform(
                    new ListInputChannel<>(null, 1, 2, 3, 4, 5),
                    new InputChannels.TransformFunction<Integer, Long>() {
                        @Override
                        public Long apply(Integer from) throws VException {
                            return (long) from + 1;
                        }
                    });
            VIterable<Long> it = InputChannels.asIterable(chan);
            assertThat(it).containsExactly(2l, 3l, 4l, 5l, 6l);
            assertThat(it.error()).isNull();
        }
        {
            VException error = new VException("boo");
            InputChannel<Long> chan = InputChannels.transform(
                    new ListInputChannel<>(error, 1, 2, 3, 4, 5),
                    new InputChannels.TransformFunction<Integer, Long>() {
                        @Override
                        public Long apply(Integer from) throws VException {
                            return (long) from + 1;
                        }
                    });
            VIterable<Long> it = InputChannels.asIterable(chan);
            assertThat(it).containsExactly(2l, 3l, 4l, 5l, 6l);
            assertThat(it.error()).isEqualTo(error);
        }
        {
            final VException error = new VException("boo");
            InputChannel<Long> chan = InputChannels.transform(
                    new ListInputChannel<>(error, 1, 2, 3, 4, 5),
                    new InputChannels.TransformFunction<Integer, Long>() {
                        @Override
                        public Long apply(Integer from) throws VException {
                            if (from > 3) {
                                throw error;
                            }
                            return (long) from + 1;
                        }
                    });
            VIterable<Long> it = InputChannels.asIterable(chan);
            assertThat(it).containsExactly(2l, 3l, 4l);
            assertThat(it.error()).isEqualTo(error);
        }
        {
            InputChannel<Integer> chan = InputChannels.transform(
                    new ListInputChannel<>(null, 1, 2, 3, 4, 5),
                    new InputChannels.TransformFunction<Integer, Integer>() {
                        @Override
                        public Integer apply(Integer from) throws VException {
                            return from == 3 ? null : from;
                        }
                    });
            VIterable<Integer> it = InputChannels.asIterable(chan);
            assertThat(it).containsExactly(1, 2, 4, 5);
            assertThat(it.error()).isNull();
        }
    }

    public void testAsList() throws Exception {
        {
            InputChannel<Integer> chan = new ListInputChannel<>(null, 1, 2, 3, 4, 5);
            assertThat(sync(InputChannels.asList(chan))).containsExactly(1, 2, 3, 4, 5);
        }
        {
            VException error = new VException("boo");
            InputChannel<Integer> chan = new ListInputChannel<>(error, 1, 2, 3, 4, 5);
            try {
                sync(InputChannels.asList(chan));
                fail("Expected InputChannels.asList() to fail");
            } catch (VException e) {
                assertThat(e).isEqualTo(error);
            }
        }
        {
            final VException error = new VException("boo");
            InputChannel<Integer> chan = InputChannels.transform(
                    new ListInputChannel<>(error, 1, 2, 3, 4, 5),
                    new InputChannels.TransformFunction<Integer, Integer>() {
                        @Override
                        public Integer apply(Integer from) throws VException {
                            if (from > 2) {
                                throw error;
                            }
                            return from;
                        }
                    });
            try {
                sync(InputChannels.asList(chan));
                fail("Expected InputChannels.asList() to fail");
            } catch (VException e) {
                assertThat(e).isEqualTo(error);
            }
        }
    }

    private static class ListInputChannel<T> implements InputChannel<T> {
        private final List<T> input;
        private int index;
        private final VException error;

        @SafeVarargs
        ListInputChannel(VException error, T... elems) {
            this.input = Arrays.asList(elems);
            this.index = 0;
            this.error = error;
        }
        @Override
        public ListenableFuture<T> recv() {
            if (index >= input.size()) {
                return Futures.immediateFailedFuture(
                        error != null ? error : new EndOfFileException(null));
            }
            return Futures.immediateFuture(input.get(index++));
        }
    }
}