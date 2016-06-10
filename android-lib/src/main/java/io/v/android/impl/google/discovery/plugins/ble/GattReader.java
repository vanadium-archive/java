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
import android.os.Build;
import android.util.Log;

import com.google.common.collect.Queues;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * A reader that reads Vanadium Gatt services from remote Gatt servers.
 */
class GattReader extends BluetoothGattCallback {
    private static final String TAG = Driver.TAG;

    // A handler that will get called when a GATT service is read.
    interface Handler {
        void onGattRead(BluetoothDevice device, BluetoothGattService service);

        void onGattReadFailed(BluetoothDevice device);
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

    private final ScheduledThreadPoolExecutor mExecutor;

    private final Set<UUID> mScanUuids;
    private final UUID mScanBaseUuid, mScanMaskUuid;
    private final Handler mHandler;

    private final ArrayDeque<BluetoothDevice> mPendingReads;

    private BluetoothDevice mCurrentDevice;
    private BluetoothGatt mCurrentGatt;
    private ScheduledFuture mCurrentGattConnectionTimeout;
    private BluetoothGattService mCurrentService;
    private Iterator<BluetoothGattService> mCurrentServiceIterator;
    private Iterator<BluetoothGattCharacteristic> mCurrentCharacteristicIterator;

    /**
     * Creates a new Gatt reader.
     * <p/>
     *
     * An empty uuids means all Vanadium services and baseUuid and maskUuid will be used to
     * filter Vanadium services.
     */
    GattReader(Context context, Set<UUID> uuids, UUID baseUuid, UUID maskUuid, Handler handler) {
        mContext = context;
        mExecutor = new ScheduledThreadPoolExecutor(1);
        mExecutor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        mScanUuids = uuids;
        mScanBaseUuid = baseUuid;
        mScanMaskUuid = maskUuid;
        mHandler = handler;
        mPendingReads = Queues.newArrayDeque();
    }

    /**
     * Reads a specified service from a remote device as well as their characteristics.
     * <p/>
     * This is an asynchronous operation. Once service read is completed, the onGattRead() or
     * onServiceReadFailed() callback is triggered.
     */
    synchronized void readDevice(BluetoothDevice device) {
        mPendingReads.add(device);
        if (mCurrentDevice == null) {
            maybeReadNextDevice();
        }
    }

    /**
     * Closes the Gatt reader cancelling the current read and deleting all pending requests.
     */
    synchronized void close(boolean graceful) {
        mPendingReads.clear();
        if (graceful) {
            // Wait until the current read finishes to avoid messing up the Bluetooth stack.
            while (mCurrentDevice != null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        mExecutor.shutdown();
        if (mCurrentGatt != null) {
            mCurrentGatt.close();
        }
    }

    private synchronized void maybeReadNextDevice() {
        mCurrentGatt = null;
        mCurrentGattConnectionTimeout = null;
        mCurrentService = null;
        mCurrentServiceIterator = null;
        mCurrentCharacteristicIterator = null;

        mCurrentDevice = mPendingReads.poll();
        if (mCurrentDevice == null) {
            notifyAll();
            return;
        }

        // This is called from Bluetooth callbacks. It seems to be more reliable to
        // call Bluetooth APIs in a new thread although it is not clear why this helps.
        mExecutor.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        connectGatt();
                    }
                });
    }

    private synchronized void finishAndMaybeReadNextDevice() {
        mCurrentGattConnectionTimeout.cancel(false);
        mCurrentGatt.close();

        maybeReadNextDevice();
    }

    private synchronized void cancelAndMaybeReadNextDevice() {
        mCurrentGattConnectionTimeout.cancel(false);
        mCurrentGatt.close();

        final BluetoothDevice device = mCurrentDevice;
        mExecutor.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        mHandler.onGattReadFailed(device);
                    }
                });
        maybeReadNextDevice();
    }

    private synchronized void connectGatt() {
        if (mCurrentDevice == null) {
            return;
        }

        mCurrentGatt = mCurrentDevice.connectGatt(mContext, false, this);
        mCurrentGattConnectionTimeout =
                mExecutor.schedule(
                        new Runnable() {
                            @Override
                            public void run() {
                                Log.e(TAG, "gatt connection timed out: " + mCurrentDevice);
                                cancelAndMaybeReadNextDevice();
                            }
                        },
                        GATT_TIMEOUT_MS,
                        TimeUnit.MILLISECONDS);
    }

    @Override
    public synchronized void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if (gatt != mCurrentGatt) {
            // This must be for an old Gatt connection which has been already cancelled. Ignore it.
            gatt.close();
            return;
        }

        if (status != BluetoothGatt.GATT_SUCCESS || newState != BluetoothGatt.STATE_CONNECTED) {
            Log.e(TAG, "connection failed: " + mCurrentDevice + " , status: " + status);
            cancelAndMaybeReadNextDevice();
            return;
        }

        // Reset the connection timer.
        if (!mCurrentGattConnectionTimeout.cancel(false)) {
            // Already cancelled.
            return;
        }

        // MTU exchange is not allowed on a BR/EDR physical link.
        // (Bluetooth Core Specification Volume 3, Part G, 4.3.1)
        //
        // There is no way to get the actual link type. So we use the device type for it.
        // It is not clear whether DEVICE_TYPE_DUAL is on a BR/EDR physical link, but
        // it is safe to not exchange MTU for that type too.
        int deviceType = mCurrentDevice.getType();
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP
                && deviceType != BluetoothDevice.DEVICE_TYPE_CLASSIC
                && deviceType != BluetoothDevice.DEVICE_TYPE_DUAL) {
            if (!gatt.requestMtu(MTU)) {
                Log.e(TAG, "requestMtu failed: " + mCurrentDevice);
                cancelAndMaybeReadNextDevice();
            }
        } else {
            if (!gatt.discoverServices()) {
                Log.e(TAG, "discoverServices failed: " + mCurrentDevice);
                cancelAndMaybeReadNextDevice();
            }
        }
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        super.onMtuChanged(gatt, mtu, status);

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.w(TAG, "onMtuChanged failed: " + mCurrentDevice + ", status: " + status);
            cancelAndMaybeReadNextDevice();
            return;
        }

        if (!gatt.discoverServices()) {
            Log.e(TAG, "discoverServices failed: " + mCurrentDevice);
            cancelAndMaybeReadNextDevice();
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "onServicesDiscovered failed: " + mCurrentDevice + ", status: " + status);
            cancelAndMaybeReadNextDevice();
            return;
        }

        mCurrentServiceIterator = gatt.getServices().iterator();
        maybeReadNextService();
    }

    private boolean isTargetService(UUID uuid) {
        if (mScanUuids.contains(uuid)) {
            return true;
        }
        return mScanUuids.isEmpty()
                && (uuid.getMostSignificantBits() & mScanMaskUuid.getMostSignificantBits())
                        == mScanBaseUuid.getMostSignificantBits()
                && (uuid.getLeastSignificantBits() & mScanMaskUuid.getLeastSignificantBits())
                        == mScanBaseUuid.getLeastSignificantBits();
    }

    private void maybeReadNextService() {
        while (mCurrentServiceIterator.hasNext()) {
            mCurrentService = mCurrentServiceIterator.next();
            if (!isTargetService(mCurrentService.getUuid())) {
                continue;
            }

            mCurrentCharacteristicIterator = mCurrentService.getCharacteristics().iterator();
            maybeReadNextCharacteristic();
            return;
        }

        // All services have been read. Finish the current device read.
        finishAndMaybeReadNextDevice();
    }

    @Override
    public void onCharacteristicRead(
            BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, "onCharacteristicRead failed: " + mCurrentDevice + ", status: " + status);
            cancelAndMaybeReadNextDevice();
            return;
        }

        maybeReadNextCharacteristic();
    }

    private void maybeReadNextCharacteristic() {
        if (!mCurrentCharacteristicIterator.hasNext()) {
            // All characteristics have been read. Finish the current service read.
            final BluetoothDevice device = mCurrentDevice;
            final BluetoothGattService service = mCurrentService;
            mExecutor.submit(
                    new Runnable() {
                        @Override
                        public void run() {
                            mHandler.onGattRead(device, service);
                        }
                    });
            maybeReadNextService();
            return;
        }

        BluetoothGattCharacteristic characteristic = mCurrentCharacteristicIterator.next();
        if (!mCurrentGatt.readCharacteristic(characteristic)) {
            Log.e(TAG, "readCharacteristic failed: " + mCurrentDevice);
            cancelAndMaybeReadNextDevice();
        }
    }
}
