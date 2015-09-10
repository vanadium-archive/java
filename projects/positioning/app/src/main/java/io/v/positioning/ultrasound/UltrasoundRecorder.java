// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.positioning.ultrasound;

import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * For now, this class only records the time its thread was started in order to simulate the ToF
 * protocol. This class will have functionality similar to the
 * {@link io.v.positioning.UltrasoundDetectorAsyncTask} class.
 */
public class UltrasoundRecorder implements Runnable {
    private static final String TAG = UltrasoundRecorder.class.getSimpleName();
    ArrayBlockingQueue<Long> timeQueue = new ArrayBlockingQueue<Long>(1);

    @Override
    public void run() {
        try {
            timeQueue.put(System.nanoTime());
        } catch (InterruptedException e) {
            Log.e(TAG, "Recorder is interrupted. " + e);
        }
    }

    public long reportTime() throws InterruptedException {
        return timeQueue.take();
    }
}
