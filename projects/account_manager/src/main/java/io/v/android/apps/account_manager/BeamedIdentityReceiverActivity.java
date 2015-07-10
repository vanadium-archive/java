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

import java.security.interfaces.ECPublicKey;

import io.v.v23.android.V;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

public class BeamedIdentityReceiverActivity extends PreferenceActivity {
    public static final String BLESSEE_PUBLIC_KEY = "BLESSEE_PUBLIC_KEY";
    public static final String TAG = "BeamBlessingsActivity";

    ECPublicKey mBlesseePubKey = null;
    Blessings[] mBlesseeBlessings = null;
    VContext mBaseContext = null;

    @Override
    public void onResume() {
        super.onResume();

        mBaseContext = V.init(this);

        // Check to see that the activity started due to a beam, and process the obtained blessings.
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            processIntent(getIntent());
        }

        // Display the public key and ask user if he wants to bless the remote end.
        Intent sendIntent = new Intent();
        sendIntent.setPackage("io.v.android.apps.account_manager");
        sendIntent.setClassName("io.v.android.apps.account_manager",
                "io.v.android.apps.account_manager.BeamBlessingActivity");
        sendIntent.setAction("io.v.android.apps.account_manager.BEAM_BLESSING");
        sendIntent.putExtra(BLESSEE_PUBLIC_KEY, mBlesseePubKey);

        PreferenceScreen prefScreen = this.getPreferenceManager().createPreferenceScreen(this);
        Preference sendBlessingPref = new Preference(this);

        // Construct the blessing names to be displayed to the user.
        String blessingNames = "";
        try {
            for (int i = 0; i < mBlesseeBlessings.length; i++) {
                // NOTE(sjayanti): This should eventually become a validated blessing name.
                blessingNames += "\n" + mBlesseeBlessings[i].toString() + "\n";
            }
        } catch (Exception error) {
            android.util.Log.e(TAG, "Public Keys on blessings didn't match: " + error);
        }

        sendBlessingPref.setSummary("Send Blessings To:\n" + blessingNames);
        sendBlessingPref.setEnabled(true);
        sendBlessingPref.setIntent(sendIntent);
        prefScreen.addPreference(sendBlessingPref);

        setPreferenceScreen(prefScreen);
    }
    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    private void processIntent(Intent intent) {
        // Retrieve the beamed public key of the remote end.
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
        mBlesseeBlessings = new Blessings[records.length];

        try {
            for (int i = 0; i < records.length; i++) {
                String blessingVom = new String(records[i].getPayload());
                mBlesseeBlessings[i] =
                        (Blessings) VomUtil.decodeFromString(blessingVom, Blessings.class);
            }
            mBlesseePubKey = mBlesseeBlessings[0].publicKey();
        } catch (VException error) {
            android.util.Log.e(TAG, "Didn't Receive Blessee Identity: " + error);
        }
    }
}
