// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.discovery.plugins.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * An advertiser that broadcasts Vanadium services through Bluetooth.
 */
class BluetoothScanner {
    private static final String TAG = Driver.TAG;

    // A handler that will get called when a GATT service is read.
    interface Handler {
        void onBluetoothDiscoveryFinished(Map<BluetoothDevice, Integer> found);
    }

    private final Context mContext;

    private final BluetoothAdapter mBluetoothAdapter;
    private BluetoothScanReceiver mBluetoothScanReceiver;

    private Set<UUID> mScanUuids;
    private Handler mHandler;

    private final class BluetoothScanReceiver extends BroadcastReceiver {
        // A map of devices that have been discovered with RSSI.
        private final Map<BluetoothDevice, Integer> mScanSeens = new HashMap<>();

        @Override
        public void onReceive(Context context, Intent intent) {
            // We try to connect each discovered device once discovery finishes,
            // since discovery will slow down connections and unstable.
            switch (intent.getAction()) {
                case BluetoothDevice.ACTION_FOUND:
                    BluetoothDevice device =
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.getType() == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
                        mScanSeens.put(
                                device,
                                (int) intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (short) 0));
                    }
                    break;
                case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                    // Note that discovery can be finished when bluetooth is turned off.
                    if (mBluetoothAdapter.isEnabled()) {
                        new RfcommConnector(mScanSeens).start();
                    }
                    break;
            }
        }
    }

    private class RfcommConnector extends Thread {
        private final Map<BluetoothDevice, Integer> mScanSeens;

        RfcommConnector(Map<BluetoothDevice, Integer> scanSeens) {
            mScanSeens = scanSeens;
        }

        public void run() {
            Map<BluetoothDevice, Integer> found = new HashMap<>();

            for (Map.Entry<BluetoothDevice, Integer> seen : mScanSeens.entrySet()) {
                BluetoothDevice device = seen.getKey();

                try (BluetoothSocket socket =
                        device.createInsecureRfcommSocketToServiceRecord(Constants.SDP_UUID)) {
                    socket.connect();

                    try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
                        synchronized (BluetoothScanner.this) {
                            if (mScanUuids == null) {
                                // Scan already finished.
                                return;
                            }
                            out.writeObject(mScanUuids);
                        }
                        out.flush();

                        if (in.readBoolean()) {
                            found.put(device, seen.getValue());
                        }
                    }
                } catch (Exception e) {
                }
            }

            synchronized (BluetoothScanner.this) {
                if (mHandler != null) {
                    mHandler.onBluetoothDiscoveryFinished(found);
                }
            }
        }
    }

    BluetoothScanner(Context context, BluetoothAdapter bluetoothAdapter, Handler handler) {
        mContext = context;
        mBluetoothAdapter = bluetoothAdapter;
        mHandler = handler;
    }

    synchronized void startScan(Set<UUID> uuids) {
        mScanUuids = uuids;

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        mBluetoothScanReceiver = new BluetoothScanReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        mContext.registerReceiver(mBluetoothScanReceiver, intentFilter);

        if (!mBluetoothAdapter.startDiscovery()) {
            mHandler.onBluetoothDiscoveryFinished(Collections.<BluetoothDevice, Integer>emptyMap());
        }
    }

    synchronized void stopScan() {
        if (mScanUuids == null) {
            return;
        }

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        mContext.unregisterReceiver(mBluetoothScanReceiver);
        mBluetoothScanReceiver = null;
        mScanUuids = null;
    }

    synchronized void close() {
        stopScan();
        mHandler = null;
    }
}
