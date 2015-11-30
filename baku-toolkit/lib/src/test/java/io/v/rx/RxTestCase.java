// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.rx;

import com.google.common.base.Throwables;

import org.joda.time.Duration;
import org.junit.After;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

import java8.util.stream.Collectors;
import java8.util.stream.StreamSupport;

import static org.junit.Assert.fail;

public abstract class RxTestCase {
    public static final long BLOCKING_DELAY_MS = 250;

    public static long verificationDelay(final Duration nominal) {
        return 2 * nominal.getMillis();
    }

    private final Collection<Throwable> mErrors = new ConcurrentLinkedQueue<>();


    public void catchAsync(final Throwable t) {
        mErrors.add(t);
    }

    /**
     * Tests should call this where it make sense and to fail early if possible.
     */
    @After
    public void assertNoAsyncErrors() {
        if (!mErrors.isEmpty()) {
            fail(StreamSupport.stream(mErrors)
                    .map(Throwables::getStackTraceAsString)
                    .collect(Collectors.joining("\n")));
        }
    }
}
