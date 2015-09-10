// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.positioning.tofprotocol;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Random;

import io.v.positioning.R;
import io.v.positioning.ble.BleAdvertiser;
import io.v.positioning.ble.BleScanner;

/**
 * This activity is meant to be used to test the Time of Flight protocol.
 * You can start listening for messages on the responder side and use the "send initial message"
 * to initiate the protocol. If there are no responses due to the listener being stopped
 * you can terminate listeners by sending an interrupt.
 */
public class TofProtocolActivity extends Activity {
    public static final int ADVERTISE_TIMEOUT = 200;
    private static final String TAG = TofProtocolActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;
    private boolean mResponding;
    // Round number will be used to synchronize among multiple phones' requests
    private int mRoundNumber;
    private int mDeviceId = new Random().nextInt();
    // Two classes participating in the protocol
    private DistanceRequestInitiator distanceRequestInitiator = null;
    private DistanceRequestReceiver responder = null;
    // Advertiser and Scanner are unique (reused) for the entire application
    private BleAdvertiser mBleAdvertiser = null;
    private BleScanner mBleScanner = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tof_protocol);
        // Initialize and check if Bluetooth is supported and enabled
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            Toast.makeText(this, R.string.no_advertising_support, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        mBleAdvertiser = new BleAdvertiser(mBluetoothAdapter);
        mBleScanner = new BleScanner(mBluetoothAdapter, mDeviceId);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (responder != null) {
            responder.cancel(true);
        }
        if (distanceRequestInitiator != null) {
            distanceRequestInitiator.cancel(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_tof_protocol, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    public void onStartTofInitiator(View view) {
        if(distanceRequestInitiator != null && distanceRequestInitiator.getStatus() == AsyncTask.Status.RUNNING) {
            Toast.makeText(this, getString(R.string.busy_try_again), Toast.LENGTH_SHORT).show();
            distanceRequestInitiator.cancel(true);
            return;
        }
        distanceRequestInitiator = new DistanceRequestInitiator(mBleAdvertiser, mBleScanner, mDeviceId, mRoundNumber++);
        distanceRequestInitiator.execute(this);
    }

    public void onInterruptInitiator(View view) {
        if (distanceRequestInitiator != null) {
            distanceRequestInitiator.cancel(true);
        }
    }

    public void onToggleTofResponding(View view) {
        if (!mResponding) {
            mResponding = true;
            responder = new DistanceRequestReceiver(mBleAdvertiser, mBleScanner, mDeviceId, mRoundNumber++);
            responder.execute(this);
            ((Button) view.findViewById(R.id.tof_responding)).setText(R.string.stop_tof_responder);
            Log.d(TAG, "start responder");
        } else {
            mResponding = false;
            responder.cancel(true);
            ((Button) view.findViewById(R.id.tof_responding)).setText(R.string.start_tof_responder);
            Log.d(TAG, "stopped responder");
        }
    }
}