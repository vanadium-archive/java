// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.observables.BlockingObservable;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public abstract class RobolectricTestCase {
    private static final long TIMEOUT_SECONDS = 5;

    public static <T> BlockingObservable<T> block(final Observable<T> source) {
        return source.timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS).toBlocking();
    }

    public static <T> T first(final Observable<T> source) {
        return block(source).first();
    }
}