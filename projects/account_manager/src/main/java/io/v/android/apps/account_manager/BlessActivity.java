// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.v.v23.android.V;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.security.Caveat;
import io.v.v23.security.VPrincipal;
import io.v.v23.security.VSecurity;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * BlessActivity represents the blessing action.  This activity expects as an input a blessee's
 * identity and blesses that identity using a combination of user-selected:
 *      1) local blessings,
 *      2) caveats, and
 *      3) extension.
 * The activity subsequently attempts to log this blessing event; it does not fail if logging is
 * unsuccessful.
 */
public class BlessActivity extends Activity implements AdapterView.OnItemSelectedListener {
    public static final String TAG = "BlessActivity";
    public static final String ERROR = "ERROR";
    public static final String REPLY = "REPLY";

    // Names of intent extras that BlessActivity is expecting to receive when invoked.
    public static final String BLESSEE_PUBLIC_KEY = "BLESSEE_PUBLIC_KEY";
    public static final String BLESSEE_NAMES      = "BLESSEE_NAMES";
    public static final String BLESSEE_EXTENSION  = "BLESSEE_EXTENSION";
    public static final String BLESSEE_EXTENSION_MUTABLE = "BLESSEE_EXTENSION_MUTABLE";

    /* The logging scheme for remote blessings is as follows. **************************************
     *      The SharedPreferences file LOG_REMOTE_PRINCIPALS stores the public keys of blessed
     *          principals.
     *      The SharedPreferences file LOG_REMOTE_BLESSINGS stores serialized data for the blessings
     *          that the principals were blessed with.
     *
     *      LOG_REMOTE_PRINCIPALS is structured as follows:
     *          The NUM_REMOTE_PRINCIPALS_KEY has an int value n that corresponds to the number of
     *              principals blessed hereto by the account manager app.
     *          The public keys of these principals are keyed by
     *              REMOTE_PRINCIPAL_KEY_0 ... REMOTE_PRINCIPAL_KEY_(n-1),
     *              where REMOTE_PRINCIPAL_KEY is the static string defined below.
     *          Furthermore, the blessing names that the holder presented initially to get blessed
     *              are stored at REMOTE_PRINCIPAL_NAMES_KEY_0 ... REMOTE_PRINCIPAL_NAMES_KEY_(n-1).
     *
     *      LOG_REMOTE_BLESSINGS contains the actual blessing data indexed as follows:
     *          The number of blessings given to a principal with public key - pk - is listed in
     *              LOG_REMOTE_PRINCIPALS.  Let us say m blessings were given out.
     *          Only MAX_BLESSINGS_FOR_REMOTE_PRINCIPAL blessing events are maintained, so the
     *              string serializations of stored blessing events are keyed by
     *              pk_(m - MAX_BLESSINGS_FOR_REMOTE_PRINCIPAL) ... pk_(m-1), if m is greater than
     *              MAX_BLESSINGS_FOR_REMOTE_PRINCIPAL, and pk_0 ... pk_(m-1) otherwise.
     *
     * Rationale for the scheme:
     *      A seemingly simpler implementation would have maintained a map of the form:
     *          pk --> Set of Blessings
     *          in the shared preferences file; however this would make adding to the log take
     *          linear time, as the stored set could not be added to directly.  Therefore, in order
     *          to add to the set one would need to copy the whole set, add to the copy, and then
     *          re-insert the new set in the map.
     *      Our scheme clearly cuts this time complexity down.
     **********************************************************************************************/
    public static final String LOG_PRINCIPALS = "LOG_PRINCIPALS";
    public static final String LOG_BLESSINGS  = "LOG_BLESSINGS";
    public static final String NUM_PRINCIPALS_KEY  = "numRemotePrincipals";
    public static final String PRINCIPAL_KEY       = "principal";
    public static final String PRINCIPAL_NAMES_KEY = "principalName";
    public static final int MAX_BLESSINGS_FOR_REMOTE_PRINCIPAL = 50;

    private static final int BLESSING_CHOOSING_REQUEST = 1;

    VContext mBaseContext = null;
    Blessings mWithBlessings = null;
    ECPublicKey mBlesseePublicKey = null;
    String[] mBlesseeNames = null;
    String mExtension = "";
    boolean mExtensionMutable = true;
    ProgressDialog mDialog = null;
    String mBlessingsVom = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBaseContext = V.init(this);

        Intent intent = getIntent();
        if (intent == null) {
            replyWithError("Intent not found.");
            return;
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            replyWithError("No extras received.");
            return;
        }
        mBlesseeNames = intent.getStringArrayExtra(BLESSEE_NAMES);
        if (mBlesseeNames == null || mBlesseeNames.length <= 0) {
            replyWithError("Blessee names not received.");
            return;
        }
        mBlesseePublicKey = (ECPublicKey) extras.get(BLESSEE_PUBLIC_KEY);
        if (mBlesseePublicKey == null) {
            replyWithError("No public key received");
            return;
        }
        mExtension = intent.getStringExtra(BLESSEE_EXTENSION);
        if (mExtension == null) {
            mExtension = "";
        }
        mExtensionMutable = intent.getBooleanExtra(BLESSEE_EXTENSION_MUTABLE, true);

        // Choose blessings to extend.
        startActivityForResult(
                new Intent(this, BlessingChooserActivity.class), BLESSING_CHOOSING_REQUEST);
    }

    private void addCaveatView() {
        // Create new caveat view.
        LinearLayout caveatView =
                (LinearLayout) getLayoutInflater().inflate(R.layout.caveat, null);
        // Set the list of supported caveats.
        Spinner spinner = (Spinner) caveatView.findViewById(R.id.caveat_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.caveats_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0, false);
        spinner.setOnItemSelectedListener(this);
        // Add caveat view.
        ((LinearLayout) findViewById(R.id.caveats)).addView(caveatView);
    }

    private void removeCaveatView(View caveatView) {
        ((LinearLayout) findViewById(R.id.caveats)).removeView(caveatView);
    }

    public void onAccept(View view) {
        try {
            List<Caveat> caveats = getCaveats();
            String extension = getExtension();
            if (extension.isEmpty()) {
                Toast.makeText(this, "Must enter non-empty extension.", Toast.LENGTH_LONG).show();
                return;
            } else {
                try {
                    log(VomUtil.encodeToString(mWithBlessings, Blessings.class), caveats,
                            extension);
                } catch (Exception e) {
                    String msg = "Couldn't log blessing event: " + e.getMessage();
                    android.util.Log.e(TAG, msg);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
                bless(extension, caveats);
            }
        } catch (VException e) {
            replyWithError("Could not get caveats.");
        }
    }
    private String encodePubKeyToString(ECPublicKey publicKey) throws IOException{
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ObjectOutputStream objOut = new ObjectOutputStream(byteOut);
        objOut.writeObject(publicKey);
        objOut.close();
        byte[] byteArray = byteOut.toByteArray();
        String stringEncoding = VomUtil.bytesToHexString(byteArray);
        return stringEncoding;
    }

    public void onDeny(View view) {
        replyWithError("User denied blessing request.");
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        LinearLayout caveatView = (LinearLayout) parent.getParent();
        String caveatType = (String) parent.getItemAtPosition(pos);
        if (caveatType.equals("None")) {
            removeCaveatView(caveatView);
            return;
        }
        if (caveatType.equals("Expiry")) {
            LinearLayout expiryView = (LinearLayout)
                    getLayoutInflater().inflate(R.layout.expiry_caveat, null);
            Spinner spinner = (Spinner) expiryView.findViewById(R.id.expiry_units_spinner);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                    R.array.time_units_array, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            caveatView.addView(expiryView);
        }
        addCaveatView();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing.
    }

    private String getExtension() {
        TextView extensionView = (EditText) findViewById(R.id.nameEditText);
        return extensionView.getText().toString();
    }

    private List<Caveat> getCaveats() throws VException {
        LinearLayout caveatsView = (LinearLayout) findViewById(R.id.caveats);
        ArrayList<Caveat> ret = new ArrayList<Caveat>();
        for (int i = 0; i < caveatsView.getChildCount(); ++i) {
            LinearLayout caveatView = (LinearLayout) caveatsView.getChildAt(i);
            AdapterView<?> caveatTypeView =
                    (AdapterView<?>) caveatView.findViewById(R.id.caveat_spinner);
            if (caveatTypeView == null) {
                android.util.Log.e(TAG, "Null caveat type spinner!");
                continue;
            }
            String caveatType = (String) caveatTypeView.getSelectedItem();
            if (caveatType == null) {
                android.util.Log.e(TAG, "Null caveat type!");
                continue;
            }
            Caveat caveat = null;
            if (caveatType.equals("None")) {
                continue;
            }
            if (caveatType.equals("Expiry")) {
                if ((caveat = getExpiryCaveat(caveatView)) == null) {
                    android.util.Log.e(TAG, "Ignoring expiry caveat.");
                    continue;
                }
            }
            ret.add(caveat);
        }
        // No caveats selected: add an unconstrained caveat.
        if (ret.isEmpty()) {
            ret.add(VSecurity.newUnconstrainedUseCaveat());
        }
        return ret;
    }

    private Caveat getExpiryCaveat(LinearLayout caveatView) {
        EditText numberUnitsView =
                (EditText) caveatView.findViewById(R.id.expiry_units);
        AdapterView<?> unitsView =
                (AdapterView<?>) caveatView.findViewById(R.id.expiry_units_spinner);
        if (numberUnitsView == null || unitsView == null) {
            android.util.Log.e(TAG, "Couldn't find expiry caveat views");
            return null;
        }
        String numberUnitsStr = numberUnitsView.getText().toString();
        String unitStr = (String) unitsView.getSelectedItem();
        int numberUnits = 0;
        try {
            numberUnits = Integer.decode(numberUnitsStr);
        } catch (NumberFormatException e) {
            android.util.Log.e(TAG, String.format("Can't parse expiry caveat units number %s: %s",
                    numberUnitsStr, e.getMessage()));
            return null;
        }
        DateTime expiry = null;
        if (unitStr.equals("Seconds")) {
            expiry = DateTime.now().plusSeconds(numberUnits);
        } else if (unitStr.equals("Minutes")) {
            expiry = DateTime.now().plusMinutes(numberUnits);
        } else if (unitStr.equals("Hours")) {
            expiry = DateTime.now().plusHours(numberUnits);
        } else if (unitStr.equals("Days")) {
            expiry = DateTime.now().plusDays(numberUnits);
        } else if (unitStr.equals("Years")) {
            expiry = DateTime.now().plusYears(numberUnits);
        } else {
            android.util.Log.e(TAG, "Unrecognized expiry caveat unit: " + unitStr);
            return null;
        }
        try {
            return VSecurity.newExpiryCaveat(expiry);
        } catch (VException e) {
            android.util.Log.e(TAG, "Couldn't create expiry caveat: " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BLESSING_CHOOSING_REQUEST:
                if (resultCode != RESULT_OK) {
                    replyWithError("Error choosing blessings: " + data.getStringExtra(ERROR));
                    return;
                }
                String blessingsVom = data.getStringExtra(REPLY);
                if (blessingsVom == null || blessingsVom.isEmpty()) {
                    replyWithError("No blessings selected.");
                    return;
                }
                try {
                    mWithBlessings =
                            (Blessings) VomUtil.decodeFromString(blessingsVom, Blessings.class);
                } catch (Exception e) {
                    replyWithError("Couldn't deserialize blessings");
                }
                if (mWithBlessings == null || mWithBlessings.isEmpty()) {
                    replyWithError("No blessings selected");
                }
                display();
                break;
        }
    }

    private void display() {
        setContentView(R.layout.activity_bless);

        TextView blesseeNames = (TextView) findViewById(R.id.text_application);
        String names = "Blessee Names:";
        for (String name : mBlesseeNames) {
            names += "\n" + name;
        }
        blesseeNames.setText(names);

        String[] blessingNames = mWithBlessings.toString().split(",");
        String blessingNamesToShow = "";
        for (String name: blessingNames) {
            blessingNamesToShow += "\n" + name;
        }

        TextView accountView =
                (TextView) getLayoutInflater().inflate(R.layout.blessing_account, null);
        accountView.setText(blessingNamesToShow);
        ((LinearLayout) findViewById(R.id.blessing_accounts)).addView(accountView);

        try {
            addCaveatView();
        } catch (Exception e) {
            replyWithError("Failed to get caveats.");
        }

        EditText extensionText = (EditText) findViewById(R.id.nameEditText);
        extensionText.setText(mExtension);
        extensionText.setEnabled(mExtensionMutable);
    }

    private void bless(String extension, List<Caveat> caveats) {
        mDialog = new ProgressDialog(this);
        mDialog.setMessage("Creating blessings...");
        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDialog.setIndeterminate(true);
        mDialog.show();
        new BlesserAsyncTask(extension, caveats).execute();
    }

    /**
     * Extends a set of Vanadium {@link Blessings} for the invoking activity.
     */
    private class BlesserAsyncTask extends AsyncTask<Void, Void, String> {
        String mExtension = "";
        List<Caveat> mCaveats = null;

        BlesserAsyncTask(String extension, List<Caveat> caveats) {
            mExtension = extension;
            mCaveats = caveats;
        }

        @Override
        protected String doInBackground(Void... arg) {
            try {
                VPrincipal principal = V.getPrincipal(mBaseContext);
                Blessings blessings =
                        principal.bless(mBlesseePublicKey, mWithBlessings, mExtension,
                                mCaveats.get(0),
                                mCaveats.subList(1, mCaveats.size()).toArray(new Caveat[0]));
                if (blessings == null) {
                    replyWithError("Got null blessing.");
                    return null;
                }
                if (blessings.getCertificateChains().size() <= 0) {
                    replyWithError("Got empty certificate chains.");
                    return null;
                }
                return VomUtil.encodeToString(blessings, Blessings.class);
            } catch (VException e) {
                replyWithError("Couldn't bless: " + e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String blessingsVom) {
            super.onPostExecute(blessingsVom);
            if (blessingsVom == null || blessingsVom.isEmpty()) {
                replyWithError("Couldn't bless : got empty derived blessings");
                return;
            }

            mBlessingsVom = blessingsVom;
            if (mDialog != null) {
                mDialog.dismiss();
            }
            replyWithSuccess(mBlessingsVom);
        }
    }

    private void log(String blessingsVom, List<Caveat> caveats, String extension)
            throws Exception{
        BlessingEvent newBlessingEvent =
                new BlessingEvent(mBlesseeNames, blessingsVom, DateTime.now(), caveats, extension);
        String pubKey = encodePubKeyToString(mBlesseePublicKey);

        SharedPreferences blessingsLog = getSharedPreferences(LOG_BLESSINGS, MODE_PRIVATE);
        SharedPreferences.Editor blessingsLogEditor = blessingsLog.edit();
        SharedPreferences principalsLog = getSharedPreferences(LOG_PRINCIPALS, MODE_PRIVATE);
        SharedPreferences.Editor principalsLogEditor = principalsLog.edit();

        int numEvents = blessingsLog.getInt(pubKey, 0);

        // Save the newBlessingEvent in the LOG_REMOTE_BLESSINGS file.
        String strNewEvent = newBlessingEvent.encodeToString();
        blessingsLogEditor.putString(pubKey + "_" + numEvents, strNewEvent);
        blessingsLogEditor.putInt(pubKey, numEvents + 1);
        if (!blessingsLogEditor.commit()) {
            throw new Exception("Failed to commit log changes");
        }

        // Delete stale blessing entry, if there are too many entries for the principal.
        String keyToDelete = pubKey + "_" + (numEvents - MAX_BLESSINGS_FOR_REMOTE_PRINCIPAL);
        if (numEvents > MAX_BLESSINGS_FOR_REMOTE_PRINCIPAL && blessingsLog.contains(keyToDelete)) {
            blessingsLogEditor.remove(keyToDelete);
            blessingsLogEditor.apply();
        }

        // Record that the principal with the public key pubKey and names mBlesseeNames has been
        // blessed in the LOG_REMOTE_PRINCIPALS file if this has not already been done.
        if (numEvents <= 0) {
            int numPrincipals = principalsLog.getInt(NUM_PRINCIPALS_KEY, 0);
            String principalsKey = PRINCIPAL_KEY + "_" + numPrincipals;
            String principalNamesKey = PRINCIPAL_NAMES_KEY + "_" + numPrincipals;
            principalsLogEditor.putString(principalsKey, pubKey);
            principalsLogEditor.putInt(NUM_PRINCIPALS_KEY, numPrincipals + 1);

            Set<String> blesseeNameSet = new HashSet<>();
            for (String name: mBlesseeNames) {
                blesseeNameSet.add(name);
            }

            principalsLogEditor.putStringSet(principalNamesKey, blesseeNameSet);
            principalsLogEditor.apply();
        }
    }

    private void replyWithError(String error) {
        android.util.Log.e(TAG, "Error while blessing: " + error);
        Intent intent = new Intent();
        intent.putExtra(ERROR, error);
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    private void replyWithSuccess(String blessingsVom) {
        Intent intent = new Intent();
        intent.putExtra(REPLY, blessingsVom);
        setResult(RESULT_OK, intent);
        finish();
    }
}
