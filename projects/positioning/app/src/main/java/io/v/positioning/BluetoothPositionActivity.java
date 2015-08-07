// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.positioning;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import org.json.JSONObject;

import java.net.MalformedURLException;

import io.v.positioning.gae.ServletPostAsyncTask;

/**
 * BluetoothPositionActivity is created when user clicks on "Find and record devices"
 * button from the MainActivity. It uses  {@link BluetoothAdapter} to listen to
 * devices and saves their androidId, MAC address, name, and RSSI signal strength
 * to the Datastore through {@link io.v.positioning.BluetoothProximityServlet}
 */
public class BluetoothPositionActivity extends Activity {

    private static final String TAG = BluetoothPositionActivity.class.getSimpleName();
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mRecordedDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetoothposition);
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });
        mRecordedDevices = new ArrayAdapter<String>(this, R.layout.device_name);
        ListView recordedDevicesListView = (ListView) findViewById(R.id.recorded_devices);
        recordedDevicesListView.setAdapter(mRecordedDevices);
        this.registerReceiver(mReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        this.registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    private void doDiscovery() {
        Log.d(TAG, "doDiscovery()");
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.finding_devices);
        findViewById(R.id.recorded_devices).setVisibility(View.VISIBLE);
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
        mBtAdapter.startDiscovery();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                // Element to be displayed in the list view of devices, member of ArrayAdapter
                String record = device.getName() + "\n" + device.getAddress() + "\n" + rssi;
                mRecordedDevices.add(record);
                addPositionRecord(device.getName(), device.getAddress(), String.valueOf(rssi));
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.discovery_finished);
                if (mRecordedDevices.isEmpty()) {
                    String noDevices = getResources().getText(R.string.no_devices_found).toString();
                    mRecordedDevices.add(noDevices);
                }
            }
        }
    };

    private void addPositionRecord(String deviceName, String deviceAddress, String rssi) {
        JSONObject data = new JSONObject();
        // If user didn't set a device name
        if (deviceName == null) {
            deviceName = "name missing";
        }
        try {
            data.put("remoteName", deviceName);
            data.put("remoteAddress", deviceAddress);
            data.put("remoteRssi", rssi);
            String androidId = Settings.Secure.getString(this.getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID);
            data.put("androidId", androidId);
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo wInfo = wifiManager.getConnectionInfo();
            data.put("myMacAddress", wInfo.getMacAddress());
            data.put("deviceTime", System.currentTimeMillis());
            new ServletPostAsyncTask("bluetooth", data).execute(this);
            Log.d(TAG, "Added " + androidId);
        } catch (MalformedURLException e) {
            Log.e(TAG, "Failed to create ServletPostAsyncTask." + e);
        } catch (Exception e) {
            Log.e(TAG, "Failed to add a record to GAE. " + e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        this.unregisterReceiver(mReceiver);
    }
}
