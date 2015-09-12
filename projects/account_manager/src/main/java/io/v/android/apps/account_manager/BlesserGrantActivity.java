// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.widget.Toast;

/**
 * BlesserGrantActivity represents the initial action that the blesser takes to accept a blessing
 * request from a blessee over one of the supported communication channels (e.g., Bluetooth).
 */
public class BlesserGrantActivity extends PreferenceActivity {
    public static final String TAG = "BlesserGrantActivity";

    private static final String CHANNEL_TITLE = "Grant Via";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        display();
    }

    private void display() {
        PreferenceScreen preferenceScreen = this.getPreferenceManager().createPreferenceScreen(this);
        PreferenceCategory channelCategory = new PreferenceCategory(this);
        channelCategory.setTitle(CHANNEL_TITLE);
        preferenceScreen.addPreference(channelCategory);

        // Create a Preference for each supported communication channel.
        // NFC
        Preference nfcPref = new Preference(this);
        nfcPref.setSummary("NFC");
        nfcPref.setEnabled(true);

        Intent nfcIntent = new Intent();
        nfcIntent.setPackage("io.v.android.apps.account_manager");
        nfcIntent.setClassName("io.v.android.apps.account_manager",
                "io.v.android.apps.account_manager.NfcBlesserActivity");
        nfcIntent.setAction("android.nfc.action.WAIT");
        nfcPref.setIntent(nfcIntent);
        channelCategory.addPreference(nfcPref);

        // Bluetooth
        Preference bluetoothPref = new Preference(this);
        bluetoothPref.setSummary("BLUETOOTH");
        bluetoothPref.setEnabled(true);

        Intent bluetoothIntent = new Intent();
        bluetoothIntent.setPackage("io.v.android.apps.account_manager");
        bluetoothIntent.setClassName("io.v.android.apps.account_manager",
                "io.v.android.apps.account_manager.BluetoothBlesserActivity");
        bluetoothIntent.setAction("io.v.android.apps.account_manager.BLUETOOTH_BLESSER_SEND");
        bluetoothPref.setIntent(bluetoothIntent);
        channelCategory.addPreference(bluetoothPref);

        setPreferenceScreen(preferenceScreen);
    }
}
