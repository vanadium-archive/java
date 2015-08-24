// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.ListView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

import io.v.v23.security.Blessings;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * BluetoothBlesseeActivity sends a request for blessings via bluetooth.  If blessings are granted,
 * it receives the granted blessings and stores them.
 */
public class BluetoothBlesseeActivity extends PreferenceActivity {
    public static final String TAG = "BluetoothBlesseeActvty"; // 23-character limit
    public static final String SERVER_NAME = "Vanadium Blessings Request";
    public static final String DEVICE = "DEVICE";

    private static final int ENABLE_BLUETOOTH_REQUEST = 1;

    byte[] mBlessingsVom = null;
    BluetoothAdapter mBluetoothAdapter = null;
    BluetoothSocket mSocket = null;
    BluetoothDevice mBlesser = null;
    ProgressDialog mDialog = null;
    PreferenceScreen mPreferenceScreen;
    Set<BluetoothDevice> mNeighboringDevices = null;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }
            // If a new bluetooth device was discovered in range.
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && device.getName() != null &&
                        !mNeighboringDevices.contains(device)) {
                    mNeighboringDevices.add(device);
                    Preference pref = new Preference(BluetoothBlesseeActivity.this);
                    pref.setSummary(device.getName());
                    pref.setEnabled(true);
                    pref.setOnPreferenceClickListener(mBlesserPreferenceListener);
                    Intent i = new Intent();
                    i.putExtra(DEVICE, device);
                    pref.setIntent(i);
                    mPreferenceScreen.addPreference(pref);
                    setPreferenceScreen(mPreferenceScreen);
                }
            }
        }
    };
    Preference.OnPreferenceClickListener mBlesserPreferenceListener =
            new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    mBlesser = (BluetoothDevice) preference.getIntent().getExtras().get(DEVICE);
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                    // Create a bluetooth connection with the chosen blesser.
                    setUserDialog("Connecting To Device...");
                    new CreateBluetoothConnection(mBluetoothAdapter, mBlesser,
                            Constants.MY_UUID_SECURE, SERVER_NAME) {
                        @Override
                        protected void onSuccess(BluetoothSocket socket) {
                            dismissUserDialog();
                            sendRequest(socket);
                        }
                        @Override
                        protected void onFailure(String error) {
                            dismissUserDialog();
                            handleError("Couldn't connect to device: " + error);
                            return;
                        }
                    }.execute();
                    return true;
                }
            };
    Preference.OnPreferenceClickListener mReceivePreferenceListener =
            new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    // Get the granted blessings over bluetooth.
                    setUserDialog("Getting Blessings...");
                    new ReceiveBluetoothMessage(mSocket) {
                        @Override
                        protected void onSuccess(byte[] blessingsVom) {
                            dismissUserDialog();
                            try{
                                Blessings blessings = (Blessings)
                                        VomUtil.decode(blessingsVom, Blessings.class);
                                Toast.makeText(BluetoothBlesseeActivity.this, "Blessings Received!",
                                        Toast.LENGTH_SHORT).show();
                                Intent i = new Intent();
                                i.setPackage("io.v.android.apps.account_manager");
                                i.setClassName("io.v.android.apps.account_manager",
                                        "io.v.android.apps.account_manager.StoreBlessingsActivity");
                                i.setAction("io.v.android.apps.account_manager.STORE");
                                i.putExtra(StoreBlessingsActivity.BLESSINGS, blessings);
                                startActivity(i);
                                finish();
                            } catch (VException e) {
                                handleError("Couldn't retrieve blessings: " + e.getMessage());
                                return;
                            }
                        }

                        @Override
                        protected void onFailure(String error) {
                            dismissUserDialog();
                            handleError(error);
                            return;
                        }
                    }.execute();
                    return true;
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPreferenceScreen = this.getPreferenceManager().createPreferenceScreen(this);
        mPreferenceScreen.bind(new ListView(this));
        mNeighboringDevices = new HashSet<BluetoothDevice>();
        mBlessingsVom = getIntent().getByteArrayExtra(BlesseeRequestActivity.BLESSINGS_VOM);
        if (mBlessingsVom == null || mBlessingsVom.length == 0) {
            handleError("Empty blessings.");
            return;
        }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            handleError("Bluetooth is not available");
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, ENABLE_BLUETOOTH_REQUEST);
        } else {
            getBlesser();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ENABLE_BLUETOOTH_REQUEST:
                if (resultCode != RESULT_OK) {
                    String error = data.getStringExtra(Constants.ERROR);
                    String msg = "Couldn't Activate Bluetooth: " +
                            (error != null ? error : "Error not found");
                    handleError(msg);
                    return;
                }
                // Bluetooth was enabled.
                getBlesser();
                break;
        }
    }

    private void getBlesser() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);
        startActivity(intent);

        mBluetoothAdapter.startDiscovery();
    }

    private void sendRequest(BluetoothSocket socket) {
        if (socket == null) {
            handleError("Could not connect over bluetooth.");
            return;
        }
        mSocket = socket;
        // Send the request for blessings over the connection.
        setUserDialog("Sending Request...");
        new SendBluetoothMessage(mBlessingsVom, mSocket) {
            @Override
            protected void onSuccess() {
                dismissUserDialog();
                display();
            }
            @Override
            protected void onFailure(String error) {
                dismissUserDialog();
                handleError(error);
                return;
            }
        }.execute();
    }

    private void setUserDialog(String message) {
        mDialog = new ProgressDialog(this);
        mDialog.setMessage(message);
        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDialog.setIndeterminate(true);
        mDialog.show();
    }

    private void dismissUserDialog() {
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    private void display() {
        PreferenceScreen prefScreen = this.getPreferenceManager().createPreferenceScreen(this);
        prefScreen.setOnPreferenceClickListener(mReceivePreferenceListener);
        Preference sendBlessingPref = new Preference(this);
        sendBlessingPref.setSummary("Receive Blessings Now");
        sendBlessingPref.setEnabled(true);
        sendBlessingPref.setOnPreferenceClickListener(mReceivePreferenceListener);
        prefScreen.addPreference(sendBlessingPref);
        setPreferenceScreen(prefScreen);
    }

    private void handleError(String error) {
        android.util.Log.e(TAG, error);
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        finish();
    }
}
