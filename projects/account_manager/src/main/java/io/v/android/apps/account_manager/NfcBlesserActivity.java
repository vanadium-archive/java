package io.v.android.apps.account_manager;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

/**
 * Displays a protocol description to a user attempting to grant blessings via NFC.
 */
public class NfcBlesserActivity extends PreferenceActivity {
    private static final String PROTOCOL_MESSAGE = "WAIT FOR BLESSEE TO BEAM BLESSINGS!";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceScreen prefScreen = this.getPreferenceManager().createPreferenceScreen(this);
        Preference messagePref = new Preference(this);
        messagePref.setSummary(PROTOCOL_MESSAGE);
        messagePref.setEnabled(false);
        prefScreen.addPreference(messagePref);
        setPreferenceScreen(prefScreen);
    }
}
