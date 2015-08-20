// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.positioning.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Random;

import io.v.positioning.R;

/**
 * Activity class to use for making requests for Bluetooth Low Energy
 * data advertisement and scanning
 */
public class BleActivity extends Activity {
    private static final String TAG = BleActivity.class.getSimpleName();
    private static final int TIMEOUT = 1000; // advertise for 1sec
    private final static int REQUEST_ENABLE_BT = 1;
    private boolean mScanning = false;
    private boolean mAdvertising = false;
    private BleScanner mBleScanner = null;
    private BleAdvertiser mBleAdvertiser = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);
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
        mBleScanner = new BleScanner(mBluetoothAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ble, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    public void onToggleBleScanning(View view) {
        if (!mScanning) {
            mScanning = true;
            mBleScanner.startScan();
            ((Button) view.findViewById(R.id.ble_scanning)).setText(R.string.stop_scanning);
            Log.d(TAG, "started scanning");
        } else {
            mScanning = false;
            mBleScanner.stopScan();
            ((Button) view.findViewById(R.id.ble_scanning)).setText(R.string.start_scanning);
            Log.d(TAG, "stopped scanning");
        }
    }

    public void onToggleBleAdvertising(View view) {
        if (!mAdvertising) {
            mAdvertising = true;
            // send BLE packet with random values for now
            mBleAdvertiser.startAdvertising(new BleData(new Random().nextInt(), new Random().nextInt(), System.nanoTime()), TIMEOUT);
            ((Button) view.findViewById(R.id.ble_advertising)).setText(R.string.stop_advertising);
            Log.d(TAG, "started advertising");
        } else {
            mAdvertising = false;
            mBleAdvertiser.stopAdvertising();
            ((Button) view.findViewById(R.id.ble_advertising)).setText(R.string.start_advertising);
            Log.d(TAG, "stopped advertising");
        }
    }

}
