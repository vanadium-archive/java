// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

/**
 * Executor that executes all of its commands on the Android UI thread.
 */
public class UiThreadExecutor implements Executor {
    /**
     * Singleton instance of the UiThreadExecutor.
     */
    public static final UiThreadExecutor INSTANCE = new UiThreadExecutor();

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void execute(Runnable runnable) {
        handler.post(runnable);
    }
    private UiThreadExecutor() {}
}
