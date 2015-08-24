// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Looper;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Sends a string message over the output stream of the given bluetooth socket.
 */
public abstract class SendBluetoothMessage extends AsyncTask<Void, Void, Void> {
    public static final byte END_OF_MESSAGE = '\n';

    private final byte[] mMessage;
    private final BluetoothSocket mSocket;
    private final OutputStream mOutStream;
    private String mError = null;

    SendBluetoothMessage(byte[] message, BluetoothSocket socket) {
        mMessage = message;
        mSocket = socket;
        OutputStream outStream = null;
        try {
            outStream = mSocket.getOutputStream();
        } catch (IOException e) {
            mError = e.getMessage();
            mOutStream = null;
            return;
        }
        mOutStream = outStream;
        if (mOutStream == null) {
            mError = "Could not get output stream";
            return;
        }
    }

    @Override
    protected Void doInBackground(Void... args) {
        if (mError != null) {
            return null;
        }
        try {
            Looper.prepare();
            mOutStream.write(mMessage);
            mOutStream.write(END_OF_MESSAGE);
        } catch (IOException e) {
            mError = e.getMessage();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void arg) {
        if (mError == null) {
            onSuccess();
        } else {
            onFailure(mError);
        }
    }

    // Called when the message is successfully sent.
    protected abstract void onSuccess();

    // Called when the message could not be sent.
    protected abstract void onFailure(String error);
}
