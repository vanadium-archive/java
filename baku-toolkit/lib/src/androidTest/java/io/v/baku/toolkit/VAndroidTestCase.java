// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.test.AndroidTestCase;

import org.joda.time.Duration;

import java.util.concurrent.TimeUnit;

import io.v.android.v23.V;
import io.v.debug.SyncbaseAndroidClient;
import io.v.v23.context.VContext;
import lombok.Getter;
import lombok.experimental.Accessors;
import rx.Observable;
import rx.Subscription;
import rx.observables.BlockingObservable;

@Accessors(prefix = "m")
@Getter
public class VAndroidTestCase extends AndroidTestCase {
    private static final long TIMEOUT_SECONDS = 10;
    private static final Duration BLOCKING_DELAY = Duration.millis(250);

    /**
     * Convenience method.
     */
    @SafeVarargs
    public static <T> Observable<T> parallel(final Observable<? extends T>... sources) {
        return Observable.merge(sources);
    }

    /**
     * Convenience method.
     */
    @SafeVarargs
    public static <T> Observable<T> sequence(final Observable<? extends T>... sources) {
        return Observable.concat(Observable.from(sources));
    }

    public static <T> BlockingObservable<T> block(final Observable<T> source) {
        return source.timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS).toBlocking();
    }

    public static <T> T await(final Observable<T> source) {
        return block(source).last();
    }

    public static <T> T first(final Observable<T> source) {
        return block(source).first();
    }

    public static Subscription start(final Observable<?> source) {
        return source.subscribe();
    }

    public static void pause() throws InterruptedException {
        Thread.sleep(BLOCKING_DELAY.getMillis());
    }

    private VContext mVContext;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mVContext = V.init(getContext());
        V.getPrincipal(mVContext).blessSelf("test");
    }

    @Override
    protected void tearDown() throws Exception {
        mVContext.cancel();
        super.tearDown();
    }

    public SyncbaseAndroidClient createSyncbaseClient() {
        // TODO(rosswang): zero duration after https://github.com/vanadium/issues/issues/809
        return new SyncbaseAndroidClient(getContext(), null, true, Duration.standardMinutes(2));
    }
}