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

/**
 * This class implements initiator of the relative distance request (phone A in the ToF/US protocol)
 * It sends the Ble packet X, waits 100ms for the other phone to turn on the microphone, plays an
 * audio, and waits for the BLE packet from the phone B containing the RxTimeX on the phone B
 */
public class DistanceRequestInitiator extends AsyncTask<Context, Void, Double> {
    private static final String TAG = DistanceRequestInitiator.class.getSimpleName();
    private static final double SPEED_OF_SOUND = 340.0;
    private static final int NANOSECONDS = 1000000000;

    private final int mDeviceId;
    private int mRoundNumber;
    // Scanner and Advertiser passed from the ToF Activity
    private final BleAdvertiser bleAdvertiser;
    private final BleScanner bleScanner;
    private Context mContext = null;

    public DistanceRequestInitiator(BleAdvertiser advertiser, BleScanner scanner, int deviceId, int roundNumber) {
        bleAdvertiser = advertiser;
        bleScanner = scanner;
        mRoundNumber = roundNumber;
        mDeviceId = deviceId;
    }

    @Override
    protected Double doInBackground(Context... params) {
        UltrasoundPlayer ultrasoundPlayer = new UltrasoundPlayer();
        // Phone A's local time in nanoseconds when the ultrasound was played.
        long mTimeUltrasoundPlayed = 0;
        mContext = params[0];
        // Time on the phone A when the Ble packet was prepared for sending.
        long mTimeBleSent = System.nanoTime();
        // time Ble packet was successfully advertised, recorded in the callback
        long actualBleTimeSent = 0;
        // Time delay B broadcasts (from the moment it received the Ble until it heard the ultrasound)
        long mTimeDiffFromB = 0;
        // Setting high priority to the thread to speed up the advertisement.
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        try {
            actualBleTimeSent = bleAdvertiser.startAdvertising(new BleData(mDeviceId, mRoundNumber, mTimeBleSent),
                    TofProtocolActivity.ADVERTISE_TIMEOUT);
        } catch (InterruptedException e) {
            Log.e(TAG, "Advertisement is interrupted. " + e);
            return null;
        }
        bleScanner.startScan();
        ultrasoundPlayer.run();
        try {
            mTimeUltrasoundPlayed = ultrasoundPlayer.reportTime();
        } catch (InterruptedException e) {
            Log.e(TAG, "Playing ultrasound is interrupted.");
            return null;
        }
        try {
            // Waiting for the phone B's BLE packet to be placed in the BleScanner's queue
            BleData data = bleScanner.getReceivedData();
            Log.d(TAG, "Received Ble from the queue.");
            mTimeDiffFromB = data.getTime();
        } catch (InterruptedException e) {
            Log.d(TAG, "Interrupted Scanning: " + e);
            return null;
        } finally {
            bleScanner.stopScan();
            bleAdvertiser.stopAdvertising();
        }
        return computeDistance(actualBleTimeSent + mTimeDiffFromB - mTimeUltrasoundPlayed);
    }

    @Override
    protected void onCancelled() {
        bleScanner.stopScan();
        bleAdvertiser.stopAdvertising();
        super.cancel(true);
    }

    @Override
    protected void onPostExecute(Double distance) {
        if (distance == null) {
            Toast.makeText(mContext, mContext.getString(R.string.responder_not_alive),
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(mContext, "Approximate distance is " + String.valueOf(distance), Toast.LENGTH_SHORT).show();
        }
    }

    private double computeDistance(long timeOfFlight) {
        double distance = SPEED_OF_SOUND * (timeOfFlight) / NANOSECONDS;
        Log.d(TAG, "Computed distance in m: " + distance);
        return distance;
    }
}