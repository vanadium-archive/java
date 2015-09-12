// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.Toast;

/**
 * Sends a request for blessings through an NFC tap.
 */
public class NfcBlesseeSendActivity extends PreferenceActivity
        implements NfcAdapter.CreateNdefMessageCallback {
    public static final String MIME_REQUEST = "vanadium/mime/request/blessing/string";

    private static final String READY_MESSAGE  = "BEAM TO REQUEST BLESSINGS!";
    private static final String FAILED_MESSAGE = "PLEASE TRY AGAIN!";

    byte[] mBlessingsVom = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check for the availability of the NFC Adapter and register a callback for ndef message
        // creation.
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        nfcAdapter.setNdefPushMessageCallback(this, this);

        mBlessingsVom = getIntent().getByteArrayExtra(BlesseeRequestActivity.BLESSINGS_VOM);
        if (mBlessingsVom == null || mBlessingsVom.length == 0) {
            displayFailureMessage();
        } else {
            displayReadyMessage();
        }
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        NdefRecord[] records = {NdefRecord.createMime(MIME_REQUEST, mBlessingsVom)};
        NdefMessage message = new NdefMessage(records);
        return message;
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    private void displayFailureMessage() {
        Toast.makeText(this, "Blessings Not Found!", Toast.LENGTH_LONG).show();

        // Display protocol message for user.
        PreferenceScreen preferenceScreen = this.getPreferenceManager().createPreferenceScreen(this);
        Preference failedPreference = this.getPreferenceManager().createPreferenceScreen(this);
        failedPreference.setSummary(FAILED_MESSAGE);
        failedPreference.setEnabled(false);
        preferenceScreen.addPreference(failedPreference);
        setPreferenceScreen(preferenceScreen);
    }

    private void displayReadyMessage() {
        PreferenceScreen preferenceScreen = this.getPreferenceManager().createPreferenceScreen(this);
        Preference readyPreference = new Preference(this);
        readyPreference.setSummary(READY_MESSAGE);
        readyPreference.setEnabled(false);
        preferenceScreen.addPreference(readyPreference);
        setPreferenceScreen(preferenceScreen);
    }
}
