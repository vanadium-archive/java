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
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import java.security.interfaces.ECPublicKey;

import io.v.android.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.security.VPrincipal;
import io.v.v23.security.VSecurity;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * Receives Android-beamed blessing requests from another account manager.
 */
public class NfcBlesserRecvActivity extends PreferenceActivity {
    public static final String TAG = "NfcBlesserRecvActivity";
    public static final String BLESSINGS_VOM = "BLESSINGS_VOM";

    private static final String DEFAULT_EXTENSION = "extension";
    private static final int BLESS_REQUEST = 1;

    Blessings mRemoteBlessings = null;
    ECPublicKey mRemotePublicKey = null;
    VContext mBaseContext = null;

    Preference.OnPreferenceClickListener mPreferenceListener =
            new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    startActivityForResult(preference.getIntent(), BLESS_REQUEST);
                    return true;
                }
            };

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }
    @Override
    public void onResume() {
        super.onResume();
        mBaseContext = V.init(this);

        // Check to see that the activity started due to a beam, and process the obtained blessings.
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            display(getIntent());
        } else {
            Toast.makeText(this, "Something has gone wrong\nNFC Receiver opened without NFC tap.",
                    Toast.LENGTH_LONG).show();
            return;
        }
    }

    private void display(Intent intent) {
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

        try {
            byte[] blessingsVom = records[0].getPayload();
            mRemoteBlessings = (Blessings) VomUtil.decode(blessingsVom, Blessings.class);
            mRemotePublicKey = mRemoteBlessings.publicKey();
        } catch (VException e) {
            android.util.Log.e(TAG, "Didn't Receive blessings from remote end: " + e);
        }
        // Invoke the bless activity if user wishes to bless the blessee.
        String[] blesseeNames = VSecurity.getSigningBlessingNames(mBaseContext,
                V.getPrincipal(mBaseContext), mRemoteBlessings);
        String blesseeTitles = "";
        if (blesseeNames == null || blesseeNames.length == 0) {
            blesseeTitles = "Principal: Not Recognized.";
        } else {
            blesseeTitles = blesseeNames[0];
            for (int j = 1; j < blesseeNames.length; j++) {
                blesseeTitles += "\n" + blesseeNames[j];
            }
        }
        Intent i = new Intent(this, BlessActivity.class);
        i.putExtra(BlessActivity.BLESSEE_PUBLIC_KEY, mRemotePublicKey);
        i.putExtra(BlessActivity.BLESSEE_NAMES, blesseeNames);
        i.putExtra(BlessActivity.BLESSEE_EXTENSION, DEFAULT_EXTENSION);
        i.putExtra(BlessActivity.BLESSEE_EXTENSION_MUTABLE, true);

        PreferenceScreen prefScreen = this.getPreferenceManager().createPreferenceScreen(this);
        prefScreen.setOnPreferenceClickListener(mPreferenceListener);
        PreferenceCategory sendCat = new PreferenceCategory(this);
        sendCat.setTitle("Send Blessings To:");
        prefScreen.addPreference(sendCat);

        // Display the names on the blessings sent by the requester.
        Preference sendBlessingPref = new Preference(this);
        sendBlessingPref.setSummary(blesseeTitles);
        sendBlessingPref.setEnabled(true);
        sendBlessingPref.setIntent(i);
        sendBlessingPref.setOnPreferenceClickListener(mPreferenceListener);
        sendCat.addPreference(sendBlessingPref);

        setPreferenceScreen(prefScreen);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BLESS_REQUEST:
                if (resultCode != RESULT_OK) {
                    String error = data.getStringExtra(Constants.ERROR);
                    String msg = "Bless operation failed: " +
                            (error != null ? error : "Error not found");
                    android.util.Log.e(TAG, msg);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    return;
                }
                byte[] blessingsVom = data.getByteArrayExtra(Constants.REPLY);
                if (blessingsVom == null || blessingsVom.length == 0) {
                    String msg = "Received empty blessings";
                    android.util.Log.e(TAG, msg);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent();
                intent.setPackage("io.v.android.apps.account_manager");
                intent.setClassName("io.v.android.apps.account_manager",
                        "io.v.android.apps.account_manager.NfcBlesserSendActivity");
                intent.setAction("android.nfc.action.NFC_BLESSER_SEND");
                intent.putExtra(BLESSINGS_VOM, blessingsVom);
                startActivity(intent);
                break;
        }
    }
}
