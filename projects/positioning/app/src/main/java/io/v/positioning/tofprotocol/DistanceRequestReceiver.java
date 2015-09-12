// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.positioning.tofprotocol;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import io.v.positioning.R;
import io.v.positioning.ble.BleAdvertiser;
import io.v.positioning.ble.BleData;
import io.v.positioning.ble.BleScanner;
import io.v.positioning.ultrasound.UltrasoundPlayer;
import io.v.positioning.ultrasound.UltrasoundRecorder;

/**
 * This class implements responder to the relative distance request (phone B in the ToF/US protocol)
 * It scans for the Ble packet X, turns on the microphone, waits for the ultrasound to arrive, plays
 * an audio, listens to its audio and computes the local delay from the microphone to the OS,
 * subtracts this delay from the OS time when it received the phone A's sound and sends it (RxTimeX)
 * to phone A over BLE.
 */
public class DistanceRequestReceiver extends AsyncTask<Context, Void, Void> {
    private static final String TAG = DistanceRequestReceiver.class.getSimpleName();

    private final BleScanner bleScanner;
    private final BleAdvertiser bleAdvertiser;
    private final int mDeviceId;
    private int mRoundNumber;
    // time when phone B's OS received the ultrasound
    private long mTimeUltrasoundRecorded;
    // time when the phone B received the BLE packet
    private long mLocalTime;
    // scanning for the BLE packets and responding until user stops the protocol
    private boolean keepAlive = true;
    private Context mContext = null;

    public DistanceRequestReceiver(BleAdvertiser advertiser, BleScanner scanner, int deviceId, int roundNumber) {
        bleAdvertiser = advertiser;
        bleScanner = scanner;
        mDeviceId = deviceId;
    }

    @Override
    protected Void doInBackground(Context... params) {
        mContext = params[0];
        while (keepAlive) {
            try {
                bleScanner.startScan();
                Log.d(TAG, "about to wait");
                BleData data = bleScanner.getReceivedData();
                Log.d(TAG, "received Ble from the queue");
                mRoundNumber = data.getRoundNumber();
                mLocalTime = bleScanner.getTimeDataReceived();
                // record incoming ultrasound
                UltrasoundRecorder ultrasoundRecorder = new UltrasoundRecorder();
                ultrasoundRecorder.run();
                mTimeUltrasoundRecorded = ultrasoundRecorder.reportTime();
                // compute the delay and advertise it for phone A to compute the distance
                long timeDataAdvertised = bleAdvertiser.startAdvertising(bleToUsTimeDifference(),
                        TofProtocolActivity.ADVERTISE_TIMEOUT);
                if(timeDataAdvertised == 0) {
                    Log.e(TAG, "Advertised timed out failing to send data. Wait for new requests.");
                    break;
                }
            } catch (InterruptedException e) {
                keepAlive = false;
                break;
            } finally {
                bleScanner.stopScan();
                // An advertised that was immediately stopped will still be broadcasted.
                bleAdvertiser.stopAdvertising();
            }
        }
        return null;
    }

    /**
     * Measure the delay from the time phone's microphone received the sound to the time
     * phone's OS received the sound.
     *
     * @return time if took from the last Ble signal to the time B heard ultrasound.
     * 0 if an error occurred.
     */
    private long getLocalDeviceDelay() {
        // play local ultrasound ...
        UltrasoundPlayer localUltrasoundPlayer = new UltrasoundPlayer();
        localUltrasoundPlayer.run();
        // ... record local ultrasound ...
        UltrasoundRecorder localUltrasoundRecorder = new UltrasoundRecorder();
        localUltrasoundRecorder.run();
        long timeLocalUltrasoundRecorded = 0;
        try {
            timeLocalUltrasoundRecorded = localUltrasoundRecorder.reportTime();
        } catch (InterruptedException e) {
            Log.e(TAG, "Could not record the ultrasound. " + e);
            return 0;
        }
        long timeLocalUltrasoundPlayed = 0;
        try {
            timeLocalUltrasoundPlayed = localUltrasoundPlayer.reportTime();
        } catch (InterruptedException e) {
            Log.e(TAG, "Could not play the ultrasound. " + e);
            return 0;
        }
        // ... and measure the local device delay
        return (timeLocalUltrasoundRecorded - timeLocalUltrasoundPlayed);
    }

    // Compute the time it took from the moment B heard BLE from A until B heard ultrasound from A
    private BleData bleToUsTimeDifference() {
        long localDelay = getLocalDeviceDelay();
        long timeDiff = (mTimeUltrasoundRecorded - localDelay) - mLocalTime;
        return new BleData(mDeviceId, mRoundNumber, timeDiff);
    }

    @Override
    protected void onCancelled() {
        keepAlive = false;
        bleScanner.stopScan();
        bleAdvertiser.stopAdvertising();
        super.cancel(true);
    }

    @Override
    protected void onPostExecute(Void result) {
        Toast.makeText(mContext, mContext.getString(R.string.responder_stopped), Toast.LENGTH_SHORT).show();
    }
}
