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

import java.security.interfaces.ECPublicKey;
import java.util.HashSet;
import java.util.Set;

import io.v.v23.android.V;
import io.v.v23.security.Blessings;
import io.v.v23.vom.VomUtil;

/**
 * BluetoothBlesserActivity connects to a blessee device that is requesting blessings, receives its
 * request, and grants it blessings.
 */
public class BluetoothBlesserActivity extends PreferenceActivity {
    public static final String TAG = "BluetoothBlesserAccept";
    public static final String DEVICE = "DEVICE";
    public static final String DEFAULT_EXTENSION = "ext";

    private static final int ENABLE_BLUETOOTH_REQUEST = 1;
    private static final int BLESS_REQUEST = 2;

    BluetoothAdapter mBluetoothAdapter = null;
    BluetoothSocket mSocket = null;
    BluetoothDevice mBlessee = null;
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
                    PreferenceActivity master = BluetoothBlesserActivity.this;
                    Preference devicePref = new Preference(master);
                    devicePref.setSummary(device.getName());
                    devicePref.setEnabled(true);
                    devicePref.setOnPreferenceClickListener(mDevicePreferenceListener);
                    Intent i = new Intent();
                    i.putExtra(DEVICE, device);
                    devicePref.setIntent(i);
                    mPreferenceScreen.addPreference(devicePref);
                    setPreferenceScreen(mPreferenceScreen);
                }
            }
        }
    };
    Preference.OnPreferenceClickListener mBlesseePreferenceListener =
            new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    startActivityForResult(preference.getIntent(), BLESS_REQUEST);
                    return true;
                }
            };
    Preference.OnPreferenceClickListener mDevicePreferenceListener =
            new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    mBlessee = (BluetoothDevice) preference.getIntent().getExtras().get(DEVICE);
                    if (mBluetoothAdapter.isDiscovering()) {
                        mBluetoothAdapter.cancelDiscovery();
                    }
                    setUserDialog("Connecting to Device...");
                    new AcceptBluetoothConnection(mBlessee, mBluetoothAdapter,
                            Constants.MY_UUID_SECURE) {
                        @Override
                        protected void onSuccess(BluetoothSocket socket) {
                            dismissUserDialog();
                            getRequest(socket);
                        }
                        @Override
                        protected void onFailure(String error) {
                            dismissUserDialog();
                            handleError("Couldn't connect to device: " + error);
                        }
                    }.execute();
                    return true;
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        V.init(this);

        mPreferenceScreen = this.getPreferenceManager().createPreferenceScreen(this);
        mPreferenceScreen.bind(new ListView(this));
        mNeighboringDevices = new HashSet<BluetoothDevice>();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            handleError("Bluetooth is not available");
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, ENABLE_BLUETOOTH_REQUEST);
        } else {
            getBlessee();
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
                getBlessee();
                break;
            case BLESS_REQUEST:
                if (data == null) {
                    handleError("Bless request returned without permission.");
                }
                if (resultCode != RESULT_OK) {
                    String error = data.getStringExtra(Constants.ERROR);
                    String msg = "Bless operation failed: " +
                            (error != null ? error : "Error not found");
                    handleError(msg);
                    return;
                }
                String blessingsVom = data.getStringExtra(Constants.REPLY);
                if (blessingsVom == null || blessingsVom.isEmpty()) {
                    String msg = "Received empty blessings";
                    handleError(msg);
                    return;
                }

                // Send VOM-encoded blessings to the blessee.
                setUserDialog("Sending Blessings...");
                new SendBluetoothMessage(blessingsVom, mSocket) {
                    @Override
                    protected void onSuccess() {
                        dismissUserDialog();
                    }
                    @Override
                    protected void onFailure(String error) {
                        dismissUserDialog();
                        handleError("Couldn't send blessings: " + error);
                        return;
                    }
                }.execute();
                Toast.makeText(this, "Blessings Sent!", Toast.LENGTH_SHORT).show();
                finish();
                break;
        }
    }

    private void getBlessee() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 60);
        startActivity(intent);

        mBluetoothAdapter.startDiscovery();
    }

    private void getRequest(BluetoothSocket socket) {
        if (socket == null) {
            handleError("Could not make Bluetooth Connection");
            return;
        }
        mSocket = socket;
        // Get the request from the remote end.
        setUserDialog("Receiving Request...");
        new ReceiveBluetoothMessage(mSocket) {
            @Override
            protected void onSuccess(String blessingsVom) {
                dismissUserDialog();
                bless(blessingsVom);
            }
            @Override
            protected void onFailure(String error) {
                dismissUserDialog();
                handleError("Didn't receive request: " + error);
                return;
            }
        }.execute();
    }

    private void bless(String blessingsVom) {
        Blessings remoteBlessings = null;
        try {
            remoteBlessings = (Blessings) VomUtil.decodeFromString(blessingsVom, Blessings.class);
        } catch (Exception e) {
            handleError("Couldn't get blessee credentials: " + e.getMessage());
            return;
        }
        if (remoteBlessings == null) {
            handleError("No blessings received.");
            return;
        }
        ECPublicKey remotePublicKey = remoteBlessings.publicKey();
        if (remotePublicKey == null) {
            handleError("Could not find blessee public key.");
            return;
        }
        display(remoteBlessings, remotePublicKey);
    }

    private void display(Blessings remoteBlessings, ECPublicKey remotePublicKey) {
        // Invoke the bless activity if user wishes to bless the blessee.
        Intent i = new Intent(this, BlessActivity.class);
        i.putExtra(BlessActivity.BLESSEE_PUBLIC_KEY, remotePublicKey);
        i.putExtra(BlessActivity.BLESSEE_NAMES, remoteBlessings.toString().split(","));
        i.putExtra(BlessActivity.BLESSEE_EXTENSION, DEFAULT_EXTENSION);
        i.putExtra(BlessActivity.BLESSEE_EXTENSION_MUTABLE, true);

        PreferenceScreen prefScreen = this.getPreferenceManager().createPreferenceScreen(this);
        prefScreen.setOnPreferenceClickListener(mBlesseePreferenceListener);
        Preference pref = new Preference(this);

        // Display the names on the blessings sent by the requester.
        pref.setSummary("Send Blessings To:\n" + remoteBlessings.toString());
        pref.setEnabled(true);
        pref.setIntent(i);
        pref.setOnPreferenceClickListener(mBlesseePreferenceListener);
        prefScreen.addPreference(pref);

        setPreferenceScreen(prefScreen);
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

    private void handleError(String error) {
        android.util.Log.e(TAG, error);
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        finish();
    }
}
