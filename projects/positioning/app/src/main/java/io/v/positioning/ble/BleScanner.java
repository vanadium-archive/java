// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.positioning.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;


/**
 * Wrapper class around the BluetoothLeScanner {@link android.bluetooth.le.BluetoothLeScanner}
 * used to scan for BLE packets defined in the BleData class {@link BleData}
 */
public class BleScanner {
    private static final String TAG = BleScanner.class.getSimpleName();

    private final int mDeviceId;
    private final BluetoothLeScanner mBluetoothLeScanner;
    private List<ScanFilter> mFilters;
    private long mTimeDataReceived;
    // Queue is used to synchronize on Scanning requests and successful scan callbacks.
    ArrayBlockingQueue<BleData> bleDataQueue;

    public BleScanner(BluetoothAdapter bluetoothAdapter, int deviceId) {
        mDeviceId = deviceId;
        mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        mFilters = new ArrayList<>();
        bleDataQueue = new ArrayBlockingQueue<BleData>(1);
    }

    // Start and stop scanning and a callback that has to be shared for identification
    public void startScan() {
        ParcelUuid uuidPositioning = ParcelUuid.fromString(BleAdvertiser.UUID);
        ScanFilter scanFilter = new ScanFilter.Builder().setServiceUuid(uuidPositioning).build();
        mFilters.add(scanFilter);
        ScanSettings mSettings = new ScanSettings.Builder().setReportDelay(0)
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        mBluetoothLeScanner.startScan(mFilters, mSettings, mScanCallback);
    }

    public void stopScan() {
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.flushPendingScanResults(mScanCallback);
            mBluetoothLeScanner.stopScan(mScanCallback);
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            mTimeDataReceived = System.nanoTime();
            byte[] serviceData = result.getScanRecord().getServiceData(ParcelUuid.fromString(BleAdvertiser.UUID));
            if (serviceData != null) {
                BleData data = new BleData(serviceData);
                if (data.getDeviceId() != mDeviceId) {
                    try {
                        bleDataQueue.put(data);
                    } catch (InterruptedException e) {
                        Log.d(TAG, "Interrupted queue's waiting" + e);
                    }
                    return;
                }
                Log.d(TAG, "Data scanned: " + data.toString() + " scanning time: " + mTimeDataReceived);
            } else {
                Log.e(TAG, "Service data is null.");
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Scan failed with error code: " + errorCode);
        }
    };

    public long getTimeDataReceived() {
        return mTimeDataReceived;
    }

    public BleData getReceivedData() throws InterruptedException {
        return bleDataQueue.take();
    }
}
