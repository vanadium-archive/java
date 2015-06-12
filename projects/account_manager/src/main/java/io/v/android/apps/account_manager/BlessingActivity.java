// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.io.IOException;
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
 * Mints a new set of Vanadium {@link Blessings} for the invoking application.
 */
public class BlessingActivity extends AccountAuthenticatorActivity
        implements OnItemSelectedListener {
    public static final String TAG = "BlessingActivity";

    public static final String BLESSEE_PUBKEY_KEY = "BLESSEE_PUBKEY";
    public static final String ERROR = "ERROR";
    public static final String REPLY = "REPLY";

    /* The logging scheme for blessings is as follows.
     *      The SharedPreferences file LOG_PACKAGES stores the package names of blessed apps.
     *      The SharedPreferences file LOG_BLESSINGS stores serialized data for the blessings
     *          that the apps were blessed with.
     *
     *      LOG_PACKAGES is structured as follows:
     *          The NUM_PACKAGES_KEY has an int value n that corresponds to the number of packages
     *              blessed hereto by the account manager app.
     *          The names of these packages are keyed by PACKAGE_KEY_0 ... PACKAGE_KEY_(n-1),
     *              where PACKAGE_KEY is the static string defined below.
     *
     *      LOG_BLESSINGS contains the actual blessing data indexed as follows:
     *          For a package - pkgName - listed in LOG_PACKAGES, the number of blessings
     *              given out to the corresponding app is keyed by pkgName. Let us say m blessings
     *              were given out.
     *          Only MAX_BLESSINGS_FOR_PACKAGE blessing events are maintained, so the string serializations
     *              of stored blessing events are keyed by
     *              pkgName_(m - MAX_BLESSINGS_FOR_PACKAGE) ... pkgName_(m-1), if m is greater
     *              than MAX_BLESSINGS_FOR_PACKAGE, and pkgName_0 ... pkgName_(m-1) otherwise.
     *
     * Rationale for the scheme:
     *      A seemingly simpler implementation would have maintained a map of the form:
     *          pkgName --> Set of Blessings
     *          in the shared preferences file; however this would make adding to the log take
     *          linear time, as the stored set could not be added to directly.  Therefore, in order
     *          to add to the set one would need to copy the whole set, add to the copy, and then
     *          re-insert the new set in the map.
     *      Our scheme clearly cuts this time complexity down tremendously.
     */
    public static final String LOG_BLESSINGS     = "LOG_BLESSINGS";
    public static final String LOG_PACKAGES      = "LOG_PACKAGES";
    public static final String NUM_PACKAGES_KEY  = "numPkgs";
    public static final String PACKAGE_KEY       = "pkg";
    public static final int MAX_BLESSINGS_FOR_PACKAGE = 50;

    private static final String ACCOUNT_TYPE = "io.vanadium";
    private static final int ACCOUNT_CHOOSING_REQUEST = 1;

    VContext mBaseContext = null;
    String mBlesseeName = "";
    String mBlesseePkgName = "";
    ECPublicKey mBlesseePubKey = null;
    Account[] mAccounts;
    volatile Blessings[] mBlessings;
    volatile int mCountBlessings;
    ProgressDialog mDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blessing);
        mBaseContext = V.init(this);

        // Get the name of the application invoking this activity.
        Intent intent = getIntent();
        if (intent == null || intent.getExtras() == null) {
            replyWithError("No extras provided.");
            return;
        }
        mBlesseeName = getCallingActivity().getClassName();
        if (mBlesseeName == null || mBlesseeName.isEmpty()) {
            replyWithError("Empty blessee name.");
            return;
        }
        ((TextView) findViewById(R.id.text_application)).setText(mBlesseeName);

        // Get callee package name.
        mBlesseePkgName = getCallingActivity().getPackageName();
        if (mBlesseePkgName == null || mBlesseeName.isEmpty()) {
            replyWithError("Empty blessee package name.");
            return;
        }

        // Get the public key of the application invoking this activity.
        Bundle b = getIntent().getExtras();
        mBlesseePubKey = (ECPublicKey) b.getSerializable(BLESSEE_PUBKEY_KEY);
        if (mBlesseePubKey == null) {
            replyWithError("Empty blessee public key.");
            return;
        }
        addCaveatView();

        // Ask the user to choose the Vanadium account(s) to bless with.
        startActivityForResult(
                new Intent(this, AccountChooserActivity.class), ACCOUNT_CHOOSING_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACCOUNT_CHOOSING_REQUEST:
                if (resultCode != RESULT_OK) {
                    replyWithError("Error choosing accounts: " + data.getStringExtra(ERROR));
                    return;
                }
                String[] accountNames = data.getStringArrayExtra(REPLY);
                if (accountNames == null || accountNames.length == 0) {
                    replyWithError("No accounts selected.");
                    return;
                }
                // Find accounts with the given names.
                mAccounts = new Account[accountNames.length];
                for (int i = 0; i < accountNames.length; ++i) {
                    Account acct = findAccount(accountNames[i]);
                    if (acct == null) {
                        replyWithError("Couldn't find account: " + accountNames[i]);
                        return;
                    }
                    mAccounts[i] = acct;
                }
                // Fill in the account names in the layout.
                for (Account acct : mAccounts) {
                    TextView accountView =
                            (TextView) getLayoutInflater().inflate(R.layout.blessing_account, null);
                    accountView.setText(acct.name);
                    ((LinearLayout) findViewById(R.id.blessing_accounts)).addView(accountView);
                }
        }
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
        getBlessings();
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
    public void onNothingSelected(AdapterView<?> parent) {
        // Do nothing.
    }

    private void getBlessings() {
        // Get blessings associated with the user-chosen accounts.
        mCountBlessings = 0;
        mBlessings = new Blessings[mAccounts.length];
        for (int i = 0; i < mAccounts.length; ++i) {
            final int id = i;
            final Account acct = mAccounts[i];
            AccountManager.get(this).getAuthToken(
                    acct, "Blessings", null, this, new OnTokenAcquired(id, acct),
                    new Handler(new Handler.Callback() {
                        @Override
                        public boolean handleMessage(Message msg) {
                            replyWithError(
                                    String.format("Couldn't get auth token for account %s: %s",
                                            acct.name, msg.toString()));
                            return true;
                        }
                    }));
        }
    }

    class OnTokenAcquired implements AccountManagerCallback<Bundle> {
        private final int mId;
        private final Account mAcct;

        OnTokenAcquired(int id, Account acct) {
            mId = id;
            mAcct = acct;
        }
        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            try {
                Bundle bundle = result.getResult();
                String blessingsVom = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                if (blessingsVom == null || blessingsVom.isEmpty()) {
                    replyWithError(String.format("Empty auth token for account: %s.", mAcct.name));
                    return;
                }
                Blessings blessings =
                        (Blessings) VomUtil.decodeFromString(blessingsVom, Blessings.class);
                handleBlessings(mId, blessings);
            } catch (AuthenticatorException e) {
                replyWithError(String.format("Couldn't authorize account %s: %s",
                        mAcct.name, e.getMessage()));
            } catch (OperationCanceledException e) {
                replyWithError(String.format("Authorization cancelled for account %s: %s",
                        mAcct.name, e.getMessage()));
            } catch (IOException e) {
                replyWithError(String.format("Unexpected error for account %s: %s",
                        mAcct.name, e.getMessage()));
            } catch (VException e) {
                replyWithError(String.format("Couldn't VOM-decode blessings for account %s: %s",
                        mAcct.name, e.getMessage()));
            }
        }
    }

    private synchronized void handleBlessings(int id, Blessings blessings) {
        mBlessings[id] = blessings;
        if (++mCountBlessings == mBlessings.length) {
            bless();
        }
    }

    private void bless() {
        mDialog = new ProgressDialog(this);
        mDialog.setMessage("Creating blessings...");
        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDialog.setIndeterminate(true);
        mDialog.show();
        new BlessAsyncTask().execute();
    }

    private class BlessAsyncTask extends AsyncTask<Void, Void, String> {
        private String mError = null;

        @Override
        protected String doInBackground(Void... arg) {
            try {
                Blessings with = mBlessings.length == 1
                        ? mBlessings[0]
                        : VSecurity.unionOfBlessings(mBlessings);
                VPrincipal principal = V.getPrincipal(mBaseContext);
                List<Caveat> caveats = getCaveats();
                Blessings retBlessing = principal.bless(mBlesseePubKey, with, mBlesseeName,
                        caveats.get(0), caveats.subList(1, caveats.size()).toArray(new Caveat[0]));
                if (retBlessing == null) {
                    mError = "Got null blessings after bless().";
                    return null;
                }
                if (retBlessing.getCertificateChains().size() <= 0) {
                    mError = "Got empty certificate chains after bless().";
                    return null;
                }
                String blessingVom = VomUtil.encodeToString(with, Blessings.class);
                log(blessingVom, caveats);
                return VomUtil.encodeToString(retBlessing, Blessings.class);
            } catch (VException e) {
                mError = "Couldn't bless: " + e.getMessage();
                return null;
            }
        }
        @Override
        protected void onPostExecute(String blessingsVom) {
            if (blessingsVom == null || blessingsVom.isEmpty()) {
                replyWithError(mError == null ? "Couldn't bless." : mError);
                return;
            }
            replyWithSuccess(blessingsVom);
        }
    }

    private void replyWithSuccess(String blessingsVom) {
        Intent intent = new Intent();
        intent.putExtra(REPLY, blessingsVom);
        setResult(RESULT_OK, intent);
        finish();
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    private void replyWithError(String error) {
        android.util.Log.e(TAG, "Blessing error: " + error);
        Intent intent = new Intent();
        intent.putExtra(ERROR, error);
        setResult(RESULT_CANCELED, intent);
        finish();
        if (mDialog != null) {
            mDialog.dismiss();
        }
    }

    private Account findAccount(String accountName) {
        Account[] accounts = AccountManager.get(this).getAccounts();
        for (Account account : accounts) {
            if (account.type.equals(ACCOUNT_TYPE) && account.name.equals(accountName)) {
                return account;
            }
        }
        return null;
    }

    private void log(String blessingsVom, List<Caveat> caveats) {
        BlessingEvent newBlessingEvent =
                new BlessingEvent(blessingsVom, DateTime.now(), caveats, mBlesseeName);

        SharedPreferences blessingsLog = getSharedPreferences(LOG_BLESSINGS, MODE_PRIVATE);
        SharedPreferences.Editor blessingsLogEditor = blessingsLog.edit();
        SharedPreferences pkgLog = getSharedPreferences(LOG_PACKAGES, MODE_PRIVATE);
        SharedPreferences.Editor pkgLogEditor = pkgLog.edit();

        try {
            int numEvents = blessingsLog.getInt(mBlesseePkgName, 0);

            // Save the newBlessingEvent in the LOG_BLESSINGS file.
            String strNewEvent = newBlessingEvent.encodeToString();
            blessingsLogEditor.putString(mBlesseePkgName + "_" + numEvents, strNewEvent);
            blessingsLogEditor.putInt(mBlesseePkgName, numEvents + 1);
            if (!blessingsLogEditor.commit()) {
                throw new Exception("Failed to commit log changes");
            }

            // Delete stale blessing entry, if there are too many entries for the app.
            String keyToDelete = mBlesseePkgName+ "_" + (numEvents - MAX_BLESSINGS_FOR_PACKAGE);
            if (numEvents > MAX_BLESSINGS_FOR_PACKAGE && blessingsLog.contains(keyToDelete)) {
                blessingsLogEditor.remove(keyToDelete);
                blessingsLogEditor.apply();
            }

            // Record that the app with the package name mBlesseePkgName has been blessed in the
            // LOG_PACKAGES file if this has not already been done.
            if (numEvents <= 0) {
                int numPkgs = pkgLog.getInt(NUM_PACKAGES_KEY, 0);
                String pkgKey = PACKAGE_KEY + "_" + numPkgs;
                pkgLogEditor.putString(pkgKey, mBlesseePkgName);
                pkgLogEditor.putInt(NUM_PACKAGES_KEY, numPkgs + 1);
                pkgLogEditor.apply();
            }
        } catch (Exception e) {
            replyWithError("Failed to Add new Blessing Event: " + e);
        }
    }
}
