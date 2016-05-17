// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.discovery.plugins.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;
import android.util.Pair;

import com.google.common.collect.Queues;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A reader that reads Vanadium Gatt services from remote Gatt servers.
 */
class GattReader extends BluetoothGattCallback {
    private static final String TAG = Driver.TAG;

    // A handler that will get called when a GATT service is read.
    interface Handler {
        void onServiceRead(BluetoothDevice device, BluetoothGattService service);

        void onServiceReadFailed(BluetoothDevice device, UUID uuid);
    }

    // TODO(jhahn): What's the maximum MTU size in Android?
    // Android seems to support up to 517 bytes. But there is no documentation on it.
    private static final int MTU = 512;

    // We serialize all Gatt requests. We cancel the request if it takes too
    // long or hangs in order to prevent it from blocking other tasks.
    //
    // TODO(jhahn): Revisit the timeout.
    private static final long GATT_TIMEOUT_MS = 10000; // 10 seconds.

    private final Context mContext;

    private final ExecutorService mExecutor;
    private final Timer mTimer;
    private final Handler mHandler;

    private final ArrayDeque<Pair<BluetoothDevice, UUID>> mPendingReads;

    private Pair<BluetoothDevice, UUID> mCurrentRead;
    private BluetoothGatt mCurrentGatt;
    private TimerTask mCurrentGattTimeout;
    private BluetoothGattService mCurrentService;
    private Iterator<BluetoothGattCharacteristic> mCurrentCharacteristicIterator;

    GattReader(Context context, Handler handler) {
        mContext = context;
        mExecutor = Executors.newSingleThreadExecutor();
        mTimer = new Timer();
        mHandler = handler;
        mPendingReads = Queues.newArrayDeque();
    }

    /**
     * Reads a specified service from a remote device as well as their characteristics.
     * <p/>
     * This is an asynchronous operation. Once service read is completed, the onServiceRead() or
     * onServiceReadFailed() callback is triggered.
     */
    synchronized void readService(BluetoothDevice device, UUID uuid) {
        mPendingReads.add(Pair.create(device, uuid));
        if (mCurrentRead == null) {
            maybeReadNextService();
        }
    }

    /**
     * Closes the Gatt reader cancelling the current read and deleting all pending requests.
     */
    synchronized void close() {
        if (mCurrentGatt != null) {
            mCurrentGatt.disconnect();
        }
        mTimer.cancel();
        mPendingReads.clear();
    }

    private synchronized void maybeReadNextService() {
        mCurrentGatt = null;
        mCurrentGattTimeout = null;
        mCurrentService = null;
        mCurrentCharacteristicIterator = null;

        mCurrentRead = mPendingReads.poll();
        if (mCurrentRead == null) {
            return;
        }
        mCurrentGatt = mCurrentRead.first.connectGatt(mContext, false, this);
        mCurrentGattTimeout =
                new TimerTask() {
                    @Override
                    public void run() {
                        Log.e(TAG, "gatt operation timed out: " + mCurrentRead.first);
                        cancelAndMaybeReadNextService();
                    }
                };
        mTimer.schedule(mCurrentGattTimeout, GATT_TIMEOUT_MS);
    }

    private synchronized void finishAndMaybeReadNextService() {
        mCurrentGattTimeout.cancel();
        mCurrentGatt.disconnect();

        final BluetoothDevice currentDevice = mCurrentRead.first;
        final BluetoothGattService currentService = mCurrentService;
        mExecutor.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        mHandler.onServiceRead(currentDevice, currentService);
                    }
                });
        maybeReadNextService();
    }

    private synchronized void cancelAndMaybeReadNextService() {
        mCurrentGattTimeout.cancel();
        mCurrentGatt.disconnect();

        final Pair<BluetoothDevice, UUID> currentRead = mCurrentRead;
        mExecutor.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        mHandler.onServiceReadFailed(currentRead.first, currentRead.second);
                    }
                });
        maybeReadNextService();
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);

        if (newState != BluetoothGatt.STATE_CONNECTED) {
            // Connection is disconnected. Release it.
            gatt.close();
            return;
        }

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "connectGatt failed: " + mCurrentRead.first + " , status: " + status);
            cancelAndMaybeReadNextService();
            return;
        }

        if (!gatt.requestMtu(MTU)) {
            Log.e(TAG, "requestMtu failed: " + mCurrentRead.first);

            // Try to discover services although requesting MTU fails.
            if (!gatt.discoverServices()) {
                Log.e(TAG, "discoverServices failed: " + mCurrentRead.first);
                cancelAndMaybeReadNextService();
            }
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.w(TAG, "requestMtu failed: " + mCurrentRead.first + ", status: " + status);
        }

        if (!gatt.discoverServices()) {
            Log.e(TAG, "discoverServices failed: " + mCurrentRead.first);
            cancelAndMaybeReadNextService();
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "discoverServices failed: " + mCurrentRead.first + ", status: " + status);
            cancelAndMaybeReadNextService();
            return;
        }

        mCurrentService = gatt.getService(mCurrentRead.second);
        if (mCurrentService == null) {
            Log.e(
                    TAG,
                    "service not found: " + mCurrentRead.first + ", uuid: " + mCurrentRead.second);
            cancelAndMaybeReadNextService();
            return;
        }

        mCurrentCharacteristicIterator = mCurrentService.getCharacteristics().iterator();
        maybeReadNextCharacteristic();
    }

    private void maybeReadNextCharacteristic() {
        if (!mCurrentCharacteristicIterator.hasNext()) {
            // All characteristics have been read. Finish the current read.
            finishAndMaybeReadNextService();
            return;
        }

        BluetoothGattCharacteristic characteristic = mCurrentCharacteristicIterator.next();
        if (!mCurrentGatt.readCharacteristic(characteristic)) {
            Log.e(TAG, "readCharacteristic failed: " + mCurrentRead.first);
            cancelAndMaybeReadNextService();
        }
    }

    @Override
    public void onCharacteristicRead(
            BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "readCharacteristic failed: " + mCurrentRead.first + ", status: " + status);
            cancelAndMaybeReadNextService();
            return;
        }

        maybeReadNextCharacteristic();
    }
}
