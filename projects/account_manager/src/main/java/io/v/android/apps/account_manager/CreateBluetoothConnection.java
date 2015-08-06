// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;

import java.io.IOException;
import java.util.UUID;

/**
 * Creates a bluetooth connection with the given device.
 */
public abstract class CreateBluetoothConnection extends AsyncTask<Void, Void, BluetoothSocket> {
    private final BluetoothAdapter mBluetoothAdapter;
    private final BluetoothDevice mRemoteDevice;
    private final UUID mUuid;
    private final String mName;
    private final BluetoothServerSocket mServerSocket;
    String mError = null;

    CreateBluetoothConnection(BluetoothAdapter bluetoothAdapter, BluetoothDevice remoteDevice,
                              UUID uuid, String name) {
        mBluetoothAdapter = bluetoothAdapter;
        mRemoteDevice = remoteDevice;
        mUuid = uuid;
        mName = name;
        BluetoothServerSocket serverSocket = null;
        try {
            serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(mName, mUuid);
        } catch(IOException e) {
            mServerSocket = null;
            mError = e.getMessage();
            return;
        }
        mServerSocket = serverSocket;
        if (mServerSocket == null) {
            mError = "Could not get server socket.";
            return;
        }
    }

    @Override
    protected BluetoothSocket doInBackground(Void... args) {
        if (mError != null) {
            return null;
        }
        BluetoothSocket socket = null;
        // Wait for a client to accept the connection.
        while(true) {
            try {
                socket = mServerSocket.accept();
            } catch (IOException e) {
                mError = e.getMessage();
            }

            // If the connection was accepted.
            if (socket != null) {
                if (socket.getRemoteDevice().equals(mRemoteDevice)) {
                    try {
                        mServerSocket.close();
                    } catch (IOException e) {
                        mError = e.getMessage();
                    }
                    return socket;
                }
            } else {
                mError = "Socket was null.";
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
