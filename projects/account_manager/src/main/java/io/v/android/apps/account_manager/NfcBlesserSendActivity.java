package io.v.android.apps.account_manager;

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
 * Sends the derived blessings to the blessee via NFC.
 */
public class NfcBlesserSendActivity extends PreferenceActivity
        implements NfcAdapter.CreateNdefMessageCallback {
    public static final String TAG = "NfcBlesserSendActivity";
    public static final String MIME_STRING = "vanadium/mime/grant/blessing/string";

    private static final String READY_MESSAGE  = "BEAM TO GRANT BLESSINGS!";
    private static final String FAILED_MESSAGE = "PLEASE TRY AGAIN!";

    String mBlessingsVom = null;

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

        // Get blessings to transmit.
        mBlessingsVom = getIntent().getStringExtra(NfcBlesserRecvActivity.BLESSINGS_VOM);
        if (mBlessingsVom == null || mBlessingsVom.isEmpty()) {
            displayFailureMessage();
        } else {
            displayReadyMessage();
        }
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        if (mBlessingsVom == null || mBlessingsVom.getBytes().length == 0) {
            Toast.makeText(this, "Please try again!", Toast.LENGTH_LONG).show();
            return null;
        }
        NdefRecord[] records = {NdefRecord.createMime(MIME_STRING, mBlessingsVom.getBytes())};
        return new NdefMessage(records);
    }

    private void displayFailureMessage() {
        Toast.makeText(this, "Couldn't Create Blessings!", Toast.LENGTH_LONG).show();

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
