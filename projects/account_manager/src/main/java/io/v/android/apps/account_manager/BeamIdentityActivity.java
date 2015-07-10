// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import java.io.IOException;

import io.v.v23.android.V;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

public class BeamIdentityActivity extends PreferenceActivity implements CreateNdefMessageCallback {
    public static final String MIME_STRING = "vanadium/mime/identity/string";
    public static final String TAG = "BeamIdentityActivity";

    public static final String ERROR = "ERROR";
    public static final String REPLY = "REPLY";

    private static final String ACCOUNT_TYPE = "io.vanadium";
    private static final int ACCOUNT_CHOOSING_REQUEST = 1;

    NfcAdapter mNfcAdapter = null;
    VContext mBaseContext = null;
    Account[] mAccounts = null;
    volatile Blessings[] mBlessings = null;
    volatile int mCountBlessings = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBaseContext = V.init(this);

        // Check for the availability of the NFC Adapter and register a callback for ndef message
        // creation.
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        mNfcAdapter.setNdefPushMessageCallback(this, this);

        // Ask the user to choose the Vanadium account(s) to bless with.
        startActivityForResult(
                new Intent(this, AccountChooserActivity.class), ACCOUNT_CHOOSING_REQUEST);
    }
    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        try {
            NdefRecord[] records = new NdefRecord[mBlessings.length];
            for (int i = 0; i < records.length; i++) {
                String blessingsVom = VomUtil.encodeToString(mBlessings[i], Blessings.class);
                records[i] = NdefRecord.createMime(MIME_STRING, blessingsVom.getBytes());
            }

            NdefMessage message = new NdefMessage(records);

            return message;
        } catch (Exception error) {
            android.util.Log.e(TAG, "Error Serializing Blessings to String: " + error);
            return null;
        }
    }
    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
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
                getBlessings();

                // Display protocol message for user.
                PreferenceScreen prefScreen = this.getPreferenceManager().createPreferenceScreen(this);
                Preference displayPref = new Preference(this);
                displayPref.setTitle("TAP PHONES TO BEAM IDENTITY!");
                displayPref.setEnabled(false);
                prefScreen.addPreference(displayPref);
                setPreferenceScreen(prefScreen);

                break;
        }
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
    }
}
