package io.v.android.apps.account_manager;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.widget.Toast;

/**
 * BlesseeRequestActivity represents the initial action that the blessee takes to requests a new set
 * of blessings.  In particular, it:
 *      1) asks the user to choose how to identify itself to the blesser,
 *      2) begins the communication with the blesser using one of the supported communication
 *         channels (e.g., NFC).
 */
public class BlesseeRequestActivity extends PreferenceActivity {
    public static final String TAG = "BlesseeRequestActivity";
    public static final String BLESSINGS_VOM = "BLESSINGS_VOM";

    private static final String CHANNEL_TITLE = "Request Via";
    private static final int BLESSING_CHOOSING_REQUEST = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ask the user to choose the Vanadium blessing(s) to identify itself with.
        startActivityForResult(
                new Intent(this, BlessingChooserActivity.class), BLESSING_CHOOSING_REQUEST);
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BLESSING_CHOOSING_REQUEST:
                if (resultCode != RESULT_OK) {
                    handleError("Error choosing blessings: " +
                            data.getStringExtra(Constants.ERROR));
                    return;
                }
                String blessingsVom = data.getStringExtra(Constants.REPLY);
                if (blessingsVom == null || blessingsVom.isEmpty()) {
                    handleError("No blessings selected.");
                    return;
                }
                display(blessingsVom);
                break;
        }
    }

    private void handleError(String error) {
        String msg = "Couldn't create request: " + error;
        android.util.Log.e(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    private void display(String blessingsVom) {
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
                "io.v.android.apps.account_manager.NfcBlesseeSendActivity");
        nfcIntent.setAction("android.nfc.action.NFC_BLESSEE_SEND");
        nfcIntent.putExtra(BLESSINGS_VOM, blessingsVom);
        nfcPref.setIntent(nfcIntent);
        channelCategory.addPreference(nfcPref);

        // Bluetooth
        Preference bluetoothPref = new Preference(this);
        bluetoothPref.setSummary("BLUETOOTH");
        bluetoothPref.setEnabled(true);

        Intent bluetoothIntent = new Intent();
        bluetoothIntent.setPackage("io.v.android.apps.account_manager");
        bluetoothIntent.setClassName("io.v.android.apps.account_manager",
                "io.v.android.apps.account_manager.BluetoothBlesseeActivity");
        bluetoothIntent.setAction("io.v.android.apps.account_manager.BLUETOOTH_BLESSEE_SEND");
        bluetoothIntent.putExtra(BLESSINGS_VOM, blessingsVom);
        bluetoothPref.setIntent(bluetoothIntent);
        channelCategory.addPreference(bluetoothPref);

        setPreferenceScreen(preferenceScreen);
    }
}
