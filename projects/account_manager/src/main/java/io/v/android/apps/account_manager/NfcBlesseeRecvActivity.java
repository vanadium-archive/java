// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Parcelable;
import android.widget.Toast;

import io.v.android.v23.V;
import io.v.v23.security.Blessings;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * NfcBlesseeRecvActivity Receives Android-beamed blessings sent by another account manager in
 * response to a previous request by NfcBlesseeSendActivity.
 */
public class NfcBlesseeRecvActivity extends Activity {
    public static final String TAG = "NfcBleseeRecvActivity";

    @Override
    public void onResume() {
        super.onResume();
        V.init(this);

        // Check to see that the activity started due to a beam, and process the obtained blessings.
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }
    }

    private void processIntent(Intent intent) {
        // Retrieve the blessings that were beamed by the remote end.
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
        String blessingsVom = new String(records[0].getPayload());
        try{
            Blessings blessings = (Blessings) VomUtil.decodeFromString(blessingsVom, Blessings.class);
            // TODO(sjayanti): Should get pattern from user.
            // TODO(sjayanti): Should eventually validate received blessings in some way.
            Intent i = new Intent();
            i.setPackage("io.v.android.apps.account_manager");
            i.setClassName("io.v.android.apps.account_manager",
                    "io.v.android.apps.account_manager.StoreBlessingsActivity");
            i.setAction("io.v.android.apps.account_manager.STORE");
            i.putExtra(StoreBlessingsActivity.BLESSINGS, blessings);
            startActivity(i);
        } catch (VException e) {
            String msg = "Couldn't Retrieve Blessings: " + e;
            android.util.Log.e(TAG, msg);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }
}
