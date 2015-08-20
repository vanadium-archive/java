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

/**
 * Wrapper class around the BluetoothLeAdvertiser {@link android.bluetooth.le.BluetoothLeAdvertiser}
 * used to advertise BLE packets defined in the BleData class {@link BleData}
 */
public class BleAdvertiser {
    public static final String UUID = "00002a5d-0000-1000-8000-00805f9b34fb";
    private static final String TAG = BleAdvertiser.class.getSimpleName();
    private BluetoothLeAdvertiser mBleAdvertiser = null;
    private BleData mData = null;
    private long mTimeDataSent = 0;
    private int mTimeout = 0;

    BleAdvertiser(BluetoothAdapter bluetoothAdapter) {
        mBleAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
    }

    public long startAdvertising(BleData data, int timeout) {
        this.mData = data;
        this.mTimeout = timeout;
        AdvertiseData ad = getAdvertiseData();
        Log.d(TAG, ad.toString());
        if (mBleAdvertiser != null) {
            mTimeDataSent = System.nanoTime();
            mBleAdvertiser.startAdvertising(getAdvertiseSettings(), ad,
                    getAdvertiseData(), mAdvertiseCallback);
            Log.d(TAG, "Started advertising");
        }
        return mTimeDataSent;
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
            super.onStartSuccess(settingsInEffect);
            Log.d(TAG, "Advertisement sent");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.d(TAG, "Advertisement failed with code: " + errorCode);
        }
    };
}
