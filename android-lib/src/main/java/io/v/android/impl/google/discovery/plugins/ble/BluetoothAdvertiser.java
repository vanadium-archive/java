// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.impl.google.discovery.plugins.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * An advertiser that broadcasts Vanadium services through Bluetooth.
 */
class BluetoothAdvertiser {
    private static final String TAG = Driver.TAG;

    private final Context mContext;

    private final BluetoothAdapter mBluetoothAdapter;
    private RfcommListener mRfcommListener;

    private final Set<UUID> mServices;

    private class RfcommListener extends Thread {
        private BluetoothServerSocket mServerSocket;

        public void run() {
            try {
                mServerSocket =
                        mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                                Constants.SDP_NAME, Constants.SDP_UUID);
            } catch (IOException e) {
                Log.e(TAG, "rfcomm listen failed", e);
                return;
            }

            for (; ; ) {
                try (BluetoothSocket socket = mServerSocket.accept();
                        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
                    Set<UUID> uuids = (Set<UUID>) in.readObject();

                    boolean found;
                    synchronized (BluetoothAdvertiser.this) {
                        if (uuids.isEmpty()) {
                            found = !mServices.isEmpty();
                        } else {
                            found = !Collections.disjoint(mServices, uuids);
                        }
                    }

                    out.writeBoolean(found);
                    out.flush();
                } catch (Exception e) {
                    Log.e(TAG, "rfcomm accept failed", e);
                    break;
                }
            }
        }

        void close() {
            if (mServerSocket == null) {
                return;
            }

            try {
                mServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close failed", e);
            }
        }
    }

    BluetoothAdvertiser(Context context, BluetoothAdapter bluetoothAdapter) {
        mContext = context;
        mBluetoothAdapter = bluetoothAdapter;
        mServices = new HashSet<>();
    }

    /**
     * Add a service to the Bluetooth advertiser.
     */
    synchronized void addService(UUID uuid, int discoverableDurationInSec) {
        mServices.add(uuid);

        if (discoverableDurationInSec <= 0) {
            return;
        }

        if (mRfcommListener == null) {
            mRfcommListener = new RfcommListener();
            mRfcommListener.start();
        }

        // Make it discoverable if not already in discoverable mode.
        if (mBluetoothAdapter.getScanMode()
                == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Log.w(TAG, "already in discoverable mode");
            return;
        }

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(
                BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, discoverableDurationInSec);
        discoverableIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(discoverableIntent);
    }

    /**
     * Removes a service from the Bluetooth advertiser.
     */
    synchronized void removeService(UUID uuid) {
        mServices.remove(uuid);
        if (mServices.isEmpty() && mRfcommListener != null) {
            mRfcommListener.close();
            mRfcommListener = null;
        }
    }

    /**
     * Closes the BT advertiser.
     */
    synchronized void close() {
        mServices.clear();
        if (mRfcommListener != null) {
            mRfcommListener.close();
            mRfcommListener = null;
        }
    }
}
