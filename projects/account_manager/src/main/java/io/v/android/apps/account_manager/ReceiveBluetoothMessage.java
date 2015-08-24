// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Looper;

import com.google.common.primitives.Bytes;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Receives a string message over the input stream of the given bluetooth socket.
 */
public abstract class ReceiveBluetoothMessage extends AsyncTask<Void, Void, byte[]> {
    private final BluetoothSocket mSocket;
    private final InputStream mInStream;
    private String mError = null;

    ReceiveBluetoothMessage(BluetoothSocket socket) {
        mSocket = socket;
        InputStream inStream = null;
        try {
            inStream = mSocket.getInputStream();
        } catch (IOException e) {
            mInStream = null;
            mError = e.getMessage();
            return;
        }
        mInStream = inStream;
        if (mInStream == null) {
            mError = "Could not get input stream";
            return;
        }
    }

    @Override
    protected byte[] doInBackground(Void... args) {
        if (mError != null) {
            return null;
        }
        Looper.prepare();
        ArrayList<Byte> message = new ArrayList<Byte>();
        byte lastByte;
        try {
            while (true) {
                lastByte = (byte) mInStream.read();
                if (lastByte != SendBluetoothMessage.END_OF_MESSAGE) {
                    message.add(lastByte);
                } else {
                    break;
                }
            }
            return Bytes.toArray(message);
        } catch (IOException e) {
            mError = e.getMessage();
            return null;
        }
    }

    @Override
    protected void onPostExecute(byte[] message) {
        if (mError == null) {
            onSuccess(message);
        } else {
            onFailure(mError);
        }
    }

    // Called when a message is successfully retrieved.
    protected abstract void onSuccess(byte[] message);

    // Called when a message could not be retrieved.
    protected abstract void onFailure(String error);
}
