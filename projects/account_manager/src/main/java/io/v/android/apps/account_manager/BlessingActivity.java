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
import android.content.Intent;
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

import com.google.common.reflect.TypeToken;

import org.joda.time.DateTime;

import java.io.IOException;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.List;

import io.v.v23.android.V;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.security.Caveat;
import io.v.v23.security.VCertificate;
import io.v.v23.security.VPrincipal;
import io.v.v23.security.VSecurity;
import io.v.v23.security.WireBlessings;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

public class BlessingActivity extends AccountAuthenticatorActivity
        implements OnItemSelectedListener {
    public static final String TAG = "BlessingActivity";

    public static final String BLESSEE_PUBKEY_KEY = "BLESSEE_PUBKEY";
    public static final String ERROR = "ERROR";
    public static final String REPLY = "REPLY";

    private static final String ACCOUNT_TYPE = "io.vanadium";
    private static final int ACCOUNT_CHOOSING_REQUEST = 1;

    VContext mBaseContext = null;
    Account mAccount = null;
    String mBlesseeName = "";
    ECPublicKey mBlesseePubKey = null;

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
            replyWithError("Empty blesee name.");
            return;
        }
        ((TextView) findViewById(R.id.text_application)).setText(mBlesseeName);

        // Get the public key of the application invoking this activity.
        Bundle b = getIntent().getExtras();
        mBlesseePubKey = (ECPublicKey) b.getSerializable(BLESSEE_PUBKEY_KEY);
        if (mBlesseePubKey == null) {
            replyWithError("Empty blessee public key.");
            return;
        }
        addCaveatView();

        // Ask the user to choose the Vanadium account to bless with.
        Intent accountIntent = AccountManager.newChooseAccountIntent(
                null, null, new String[]{ACCOUNT_TYPE}, true, null, null, null, null);
        startActivityForResult(accountIntent, ACCOUNT_CHOOSING_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ACCOUNT_CHOOSING_REQUEST:
                if (resultCode != RESULT_OK || data == null || data.getExtras() == null) {
                    replyWithError("Error selecting account.");
                    return;
                }
                String accountName = data.getExtras().getString(
                        AccountManager.KEY_ACCOUNT_NAME);
                if (accountName == null || accountName.isEmpty()) {
                    replyWithError("Empty account name.");
                    return;
                }
                mAccount = findAccount(accountName);
                if (mAccount == null) {
                    replyWithError(String.format(
                            "Couldn't find account %s of type %s", accountName, ACCOUNT_TYPE));
                    return;
                }
                ((TextView) findViewById(R.id.text_account)).setText(accountName);
                return;
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

    public void onAccept(@SuppressWarnings("unused") View view) {
        blessAccount();
    }

    public void onDeny(@SuppressWarnings("unused") View view) {
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

    private void blessAccount() {
        AccountManager.get(this).getAuthToken(
                mAccount, "WireBlessings", null, this, new OnTokenAcquired(),
                new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        replyWithError(String.format(
                                "Couldn't get auth token: %s", mAccount.name, msg.toString()));
                        return true;
                    }
                }));
    }

    class OnTokenAcquired implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            try {
                Bundle bundle = result.getResult();
                String blessingsVom = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                if (blessingsVom == null || blessingsVom.isEmpty()) {
                    replyWithError("Empty auth token.");
                    return;
                }
                bless(blessingsVom);
            } catch (AuthenticatorException e) {
                replyWithError("Couldn't authorize: " + e.getMessage());
            } catch (OperationCanceledException e) {
                replyWithError("Authorization cancelled: " + e.getMessage());
            } catch (IOException e) {
                replyWithError("Unexpected error: " + e.getMessage());
            }
        }
    }

    private void bless(String blessingsVom) {
        try {
            Blessings with = (Blessings) VomUtil.decodeFromString(blessingsVom, Blessings.class);
            VPrincipal principal = V.getPrincipal(mBaseContext);
            List<Caveat> caveats = getCaveats();
            Blessings retBlessing = principal.bless(mBlesseePubKey, with, mBlesseeName,
                    caveats.get(0), caveats.subList(1, caveats.size()).toArray(new Caveat[0]));

            if (retBlessing == null) {
                replyWithError("Got null blessings after bless().");
                return;
            }
            WireBlessings retWire = retBlessing.wireFormat();
            if (retWire == null) {
                replyWithError("Got null wire blessings even though blessings are non-null");
                return;
            }
            if (retWire.getCertificateChains().size() <= 0) {
                replyWithError("Got empty certificate chains.");
                return;
            }
            if (retWire.getCertificateChains().size() > 1) {
                replyWithError("Expected single certificate chain, got: " + retWire.toString());
                return;
            }
            List<VCertificate> chain = retWire.getCertificateChains().get(0);
            if (chain == null || chain.size() <= 0) {
                replyWithError("Empty certificate chain");
                return;
            }
            replyWithSuccess(retWire);
        } catch (VException e) {
            replyWithError("Couldn't bless: " + e.getMessage());
        }
    }

    private void replyWithSuccess(WireBlessings wire) {
        Intent intent = new Intent();
        intent.putExtra(REPLY, wire);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void replyWithError(String error) {
        android.util.Log.e(TAG, "Blessing error: " + error);
        Intent intent = new Intent();
        intent.putExtra(ERROR, error);
        setResult(RESULT_CANCELED, intent);
        finish();
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
}
