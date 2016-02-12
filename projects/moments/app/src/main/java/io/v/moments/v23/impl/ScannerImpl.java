// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.v23.impl;

import android.util.Log;

import com.google.common.util.concurrent.FutureCallback;

import io.v.moments.model.Config;
import io.v.moments.v23.ifc.Scanner;
import io.v.moments.v23.ifc.V23Manager;
import io.v.v23.InputChannelCallback;
import io.v.v23.context.VContext;
import io.v.v23.discovery.Update;

/**
 * Handles scanning for moments.
 *
 * This class similar to AdvertiserImpl - see that class for more commentary -
 * in that it functions as decoupling of the UX from the moving target that is
 * the V23Manager/VContext API
 *
 * At the moment, the complexity is far less than it is for advertising
 * (scanning doesn't require a running service), so this class doesn't add much
 * value over using an instance of V23ManagerImpl and VContext directly in whatever
 * class uses a Scanner.  To make this class more useful in decoupling moments
 * specific code from v23 specifics, this class could tease apart a scan Update,
 * and feed advertisement data from the Update.Found and Update.Lost object
 * directly into, say, the appropriate instances of java.util.function.Consumer<T>
 * that were passed to #start en lieu of the InputChannelCallback<Update>
 * updateCallback.  That way the caller would have no exposure to v23 classes.
 */
public class ScannerImpl implements Scanner {
    private static final String TAG = "ScannerImpl";
    private final V23Manager mV23Manager;
    private final String mQuery;
    private VContext mScanCtx;

    public ScannerImpl(V23Manager v23Manager, String query) {
        if (v23Manager == null) {
            throw new IllegalArgumentException("Null v23Manager.");
        }
        if (query == null || query.isEmpty()) {
            throw new IllegalArgumentException("Empty query.");
        }
        mV23Manager = v23Manager;
        mQuery = query;
    }

    @Override
    public String toString() {
        return "scan(" + mQuery + "," + isScanning() + ")";
    }

    /**
     * Accepts the scanning context on success (or assures that it's null on
     * failure), then activates the callback supplied by the client (likely to
     * change the UX).
     */
    private FutureCallback<VContext> makeWrapped(
            final FutureCallback<Void> startup) {
        return new FutureCallback<VContext>() {
            @Override
            public void onSuccess(VContext context) {
                mScanCtx = context;
                startup.onSuccess(null);
            }

            @Override
            public void onFailure(final Throwable t) {
                mScanCtx = null;
                startup.onFailure(t);
            }
        };
    }

    @Override
    public void start(
            FutureCallback<Void> startupCallback,
            InputChannelCallback<Update> updateCallback,
            FutureCallback<Void> completionCallback) {
        Log.d(TAG, "Entering start.");
        if (isScanning()) {
            startupCallback.onFailure(
                    new IllegalStateException("Already scanning."));
            return;
        }
        mV23Manager.scan(
                mQuery, Config.Discovery.DURATION,
                makeWrapped(startupCallback),
                updateCallback,
                completionCallback);
        Log.d(TAG, "Exiting start.");
    }

    @Override
    public boolean isScanning() {
        return mScanCtx != null && !mScanCtx.isCanceled();
    }

    @Override
    public void stop() {
        Log.d(TAG, "Entering stop");
        if (mScanCtx != null) {
            Log.d(TAG, "Cancelling scan.");
            if (!mScanCtx.isCanceled()) {
                mScanCtx.cancel();
            }
            mScanCtx = null;
        }
        Log.d(TAG, "Exiting stop");
    }
}
