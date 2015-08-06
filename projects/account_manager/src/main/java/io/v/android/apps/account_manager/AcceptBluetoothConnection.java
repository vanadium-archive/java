// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;

import com.google.common.base.Stopwatch;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Accepts a bluetooth connection hosted by the given server bluetooth device.
 */
public abstract class AcceptBluetoothConnection extends AsyncTask<Void, Void, BluetoothSocket> {
    private static final int TRY_FOR = 30; // Time in seconds to try connecting for.

    private final BluetoothDevice mRemoteDevice;
    private final BluetoothAdapter mBluetoothAdapter;
    private final UUID mUuid;
    private String mError = null;

    public AcceptBluetoothConnection(BluetoothDevice remoteDevice,
                                     BluetoothAdapter bluetoothAdapter, UUID uuid) {
        mRemoteDevice = remoteDevice;
        mBluetoothAdapter = bluetoothAdapter;
        mUuid = uuid;
    }

    @Override
    protected BluetoothSocket doInBackground(Void... args) {
        if (mError != null) {
            return null;
        }
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        BluetoothSocket socket = null;
        Stopwatch stopwatch = Stopwatch.createUnstarted();
        for (stopwatch.start(); stopwatch.elapsed(TimeUnit.SECONDS) <= TRY_FOR; socket = null) {
            try {
                socket = mRemoteDevice.createRfcommSocketToServiceRecord(mUuid);
                if (socket != null && socket.getRemoteDevice().equals(mRemoteDevice)) {
                    socket.connect();
                    return socket;
                }
            } catch (IOException e) {
                // Do nothing.
            }
        }
        try {
            socket = mRemoteDevice.createRfcommSocketToServiceRecord(mUuid);
            socket.connect();
            return socket;
        } catch (IOException e) {
            mError = e.getMessage();
            try {
                socket.close();
                return null;
            } catch (IOException eClose) {
                mError += "\n" + eClose.getMessage();
                return null;
            }
        }
    }

    @Override
    protected void onPostExecute(BluetoothSocket socket) {
        if (mError == null) {
            onSuccess(socket);
        } else {
            onFailure(mError);
        }
    }

    // Called when a connection is successfully established.
    protected abstract void onSuccess(BluetoothSocket socket);

    // Called when a connection cannot be established.
    protected abstract void onFailure(String error);
}
