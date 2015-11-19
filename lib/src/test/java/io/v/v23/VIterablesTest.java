// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23;

import junit.framework.TestCase;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import io.v.v23.verror.VException;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests for static methods in {@link VIterables} class.
 */
public class VIterablesTest extends TestCase {
    @Override
    protected void setUp() throws Exception {
        V.init();
    }

    public void testTransform() throws Exception {
        assertThat(VIterables.transform(create(null, 1, 2, 3, 4, 5),
                new VIterables.TransformFunction<Integer, Long>() {
                    @Override
                    public Long apply(Integer from) throws VException {
                        return (long) from + 1;
                    }
                })).containsExactly(2l, 3l, 4l, 5l, 6l);

        {
            VException e = new VException("boo");
            VIterable<Long> it = VIterables.transform(create(e, 1, 2, 3, 4, 5),
                    new VIterables.TransformFunction<Integer, Long>() {
                        @Override
                        public Long apply(Integer from) throws VException {
                            return (long)from + 1;
                        }
                    });
            assertThat(it).containsExactly(2l, 3l, 4l, 5l, 6l);
            assertThat(it.error()).isEqualTo(e);
        }
        {
            final VException e = new VException("boo");
            VIterable<Long> it = VIterables.transform(create(null, 1, 2, 3, 4, 5),
                    new VIterables.TransformFunction<Integer, Long>() {
                        @Override
                        public Long apply(Integer from) throws VException {
                            if (from > 3) {
                                throw e;
                            }
                            return (long) from + 1;
                        }
                    });
            assertThat(it).containsExactly(2l, 3l, 4l);
            assertThat(it.error()).isEqualTo(e);
        }
    }

    private static <T> VIterable<T> create(final VException error, T... elems) {
        final List<T> list = Arrays.asList(elems);
        return new VIterable<T>() {
            @Override
            public Iterator<T> iterator() {
                return list.iterator();
            }
            @Override
            public VException error() {
                return error;
            }
        };
    }
}