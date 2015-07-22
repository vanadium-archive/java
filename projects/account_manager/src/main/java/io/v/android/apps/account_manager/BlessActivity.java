// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

// TODO(sjayanti): Merge with BlessingActivity.
package io.v.android.apps.account_manager;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
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

import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.List;

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
 */
public class BlessActivity extends Activity implements AdapterView.OnItemSelectedListener {
    public static final String TAG = "BlessActivity";
    public static final String ERROR = "ERROR";
    public static final String REPLY = "REPLY";

    private static final int BLESSING_CHOOSING_REQUEST = 1;

    VContext mBaseContext = null;
    Blessings mWithBlessings = null;
    ECPublicKey mBlesseePublicKey = null;
    String[] mRemoteEndNames = null;
    ProgressDialog mDialog = null;
    String mBlessingsVom = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBaseContext = V.init(this);

        Intent intent = getIntent();
        mRemoteEndNames = intent.getStringArrayExtra(NfcBlesserRecvActivity.REMOTE_END_NAMES);
        mBlesseePublicKey = (ECPublicKey)
                intent.getExtras().get(NfcBlesserRecvActivity.REMOTE_END_PUBLIC_KEY);

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
                bless(extension, caveats);
            }
        } catch (VException e) {
            replyWithError("Could not get caveats.");
        }
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
        for (String name: mRemoteEndNames) {
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
