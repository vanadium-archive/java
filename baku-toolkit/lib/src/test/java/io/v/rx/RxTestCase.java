// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx;

import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

import org.joda.time.Duration;
import org.junit.After;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;
import rx.Observable;
import rx.observables.BlockingObservable;

import static org.junit.Assert.fail;

public abstract class RxTestCase {
    public static final long
            BLOCKING_DELAY_MS = 2000,
            DIAGNOSTIC_DELAY_MS = 250;

    public static long verificationDelay(final Duration nominal) {
        return 2 * nominal.getMillis();
    }

    public static <T> BlockingObservable<T> block(final Observable<T> source) {
        return source.timeout(BLOCKING_DELAY_MS, TimeUnit.SECONDS).toBlocking();
    }

    public static <T> T first(final Observable<T> source) {
        return block(source).first();
    }

    private final Multimap<Class<? extends Throwable>, Throwable> mErrors =
            Multimaps.synchronizedListMultimap(ArrayListMultimap.create());


    public void catchAsync(final Throwable t) {
        mErrors.put(t.getClass(), t);
    }

    public void expect(final Class<? extends Throwable> type) {
        final Iterator<Throwable> iter = mErrors.get(type).iterator();
        if (!iter.hasNext()) {
            fail(type + " expected but not thrown");
        } else {
            iter.next();
            iter.remove();
        }
    }

    /**
     * Tests should call this where it make sense and to fail early if possible.
     */
    @After
    public void assertNoAsyncErrors() {
        if (!mErrors.isEmpty()) {
            fail(StreamSupport.stream(mErrors.values())
                    .map(Throwables::getStackTraceAsString)
                    .collect(Collectors.joining("\n")));
        }
    }
}
