// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import java.util.Map;
import java.util.Set;

import io.v.v23.android.V;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.security.Caveat;
import io.v.v23.security.VPrincipal;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

public class BeamedBlessingReceiverActivity extends PreferenceActivity {
    public static final String TAG = "BeamedBlessingReceiver";

    VContext mBaseContext = null;

    @Override
    public void onResume() {
        super.onResume();

        // Check to see that the activity started due to a beam, and process the obtained blessings.
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }

    private void processIntent(Intent intent) {
        // Retrieve the blessings that were beamed by the remote end.
        Blessings blessings = null;
        Parcelable[] rawMessages = intent.getParcelableArrayExtra(
                    NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMessages == null || rawMessages.length == 0) {
            Toast.makeText(this, "Did not receive blessing(s).", Toast.LENGTH_LONG).show();
            return;
        }
        NdefMessage message = (NdefMessage) rawMessages[0];
        NdefRecord[] records = message.getRecords();
        if (records == null || records.length == 0) {
            Toast.makeText(this, "Did not receive blessing(s).", Toast.LENGTH_LONG).show();
            return;
        }
        String blessingsVOM = new String(records[0].getPayload());

        try{
            blessings = (Blessings) VomUtil.decodeFromString(blessingsVOM, Blessings.class);
        } catch (VException error) {
            android.util.Log.e(TAG, "Couldn't Retrieve Blessings: " + error);
        }

        // Display the obtained blessings-union on screen.
        try {
            PreferenceScreen prefScreen = this.getPreferenceManager().createPreferenceScreen(this);
            mBaseContext = V.init(this);
            VPrincipal principal = V.getPrincipal(mBaseContext);
            principal.addToRoots(blessings); /* NOTE(sjayanti): Should be done elsewhere */
            Map<String, Caveat[]> blessingsMap = principal.blessingsInfo(blessings);
            Set<String> blessingNames = blessingsMap.keySet();

            // Create a new preference for each blessing received.
            // NOTE(sjayanti): These blessings should eventually be saved, once an appropriate
            //                 protocol for saving the blessings is decided upon.
            for (String name: blessingNames) {
                Preference currentPref = new Preference(this);
                currentPref.setSummary(name);
                currentPref.setEnabled(true);
                prefScreen.addPreference(currentPref);
            }
            setPreferenceScreen(prefScreen);
        } catch (VException error) {
            android.util.Log.e(TAG, "Blessing Names could not be retrieved: " + error);
        }
    }
}
