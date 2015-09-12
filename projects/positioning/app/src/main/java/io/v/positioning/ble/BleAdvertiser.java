// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.positioning.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Wrapper class around the BluetoothLeAdvertiser {@link android.bluetooth.le.BluetoothLeAdvertiser}
 * used to advertise BLE packets defined in the BleData class {@link BleData}
 */
public class BleAdvertiser {
    public static final String UUID = "00002a5d-0000-1000-8000-00805f9b34fb";
    private static final String TAG = BleAdvertiser.class.getSimpleName();
    private final BluetoothLeAdvertiser mBleAdvertiser;
    private BleData mData = null;
    private long mTimeDataSent = 0;
    private int mTimeout = 0;
    // Blocking queue is used to synchronize between the advertisement requests and their callbacks
    // Queue element is the time when successful advertisement was sent (initialized in the callback)
    private ArrayBlockingQueue<Long> advertisementSentWaiter = new ArrayBlockingQueue<Long>(1);;

    public BleAdvertiser(BluetoothAdapter bluetoothAdapter) {
        mBleAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBleAdvertiser == null) {
            throw new IllegalStateException("BLE advertiser null");
        }
    }

    /**
     * @return time advertisement was successfully sent or 0 on failure
     * */
    public long startAdvertising(BleData data, int timeout) {
        this.mData = data;
        this.mTimeout = timeout;
        AdvertiseData ad = getAdvertiseData();
        long mTimeDataSent = System.nanoTime();
        mBleAdvertiser.startAdvertising(getAdvertiseSettings(), ad,
                getAdvertiseData(), mAdvertiseCallback);
        Log.d(TAG, "Data advertised: " + mData.toString() + " at time: " + mTimeDataSent);
        // waiting for the notification from on a successful callback ...
        try{
            return advertisementSentWaiter.take();
        } catch (InterruptedException e) {
            Log.e(TAG, "Advertisement timed out.");
            return 0;
        }
    }

    public void sendAdvertisingNonBlocking(BleData data, int timeout) {
        this.mData = data;
        this.mTimeout = timeout;
        AdvertiseData ad = getAdvertiseData();
        long mTimeDataSent = System.nanoTime();
        mBleAdvertiser.startAdvertising(getAdvertiseSettings(), ad,
                getAdvertiseData(), mAdvertiseCallbackNonBlocking);
        Log.d(TAG, "Data advertised: " + mData.toString() + " at time: " + mTimeDataSent);
    }

    public void stopAdvertisingNonBlocking() {
        if (mBleAdvertiser != null) {
            mBleAdvertiser.stopAdvertising(mAdvertiseCallbackNonBlocking);
        }
    }

    private AdvertiseSettings getAdvertiseSettings() {
        AdvertiseSettings.Builder asb = new AdvertiseSettings.Builder();
        asb.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(false)
                .setTimeout(mTimeout)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        return asb.build();
    }

    private AdvertiseData getAdvertiseData() {
        AdvertiseData.Builder adb = new AdvertiseData.Builder();
        adb.setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceData(ParcelUuid.fromString(UUID), mData.packageData())
                .addServiceUuid(ParcelUuid.fromString(UUID));
        return adb.build();
    }

    public void stopAdvertising() {
        if (mBleAdvertiser != null) {
            mBleAdvertiser.stopAdvertising(mAdvertiseCallback);
        }
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "Advertisement sent successfully.");
            try {
                // ... notify the waiter that an advertisement was successfully sent
                advertisementSentWaiter.put(System.nanoTime());
            } catch (InterruptedException e) {
                Log.e(TAG, "Advertiser is interrupted. " + e);
            }
            Log.d(TAG, "Advertisement sent");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.d(TAG, "Advertisement failed with code: " + errorCode);
        }
    };

    private AdvertiseCallback mAdvertiseCallbackNonBlocking = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.d(TAG, "Advertisement sent");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.d(TAG, "Advertisement failed with code: " + errorCode);
        }
    };
}
