// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Prompts user to choose among potentially many Vanadium accounts.
 */
public class AccountChooserActivity extends Activity {
    public static final String TAG = "AccountChooserActivity";
    private static final String ACCOUNT_TYPE = "io.vanadium";

    public static final String ERROR = "ERROR";
    public static final String REPLY = "REPLY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_chooser);
        Account[] accounts = AccountManager.get(this).getAccountsByType(ACCOUNT_TYPE);
        if (accounts.length == 0) {
            // No Vanadium accounts available.
            createAccount();
            return;
        }
        for (Account account : accounts) {
            LinearLayout accountView =
                    (LinearLayout) getLayoutInflater().inflate(R.layout.chooser_account, null);
            ((CheckedTextView) accountView.findViewById(R.id.chooser_account)).setText(
                    account.name);
            ((LinearLayout) findViewById(R.id.chooser_accounts)).addView(accountView);
        }
    }

    // Starts the account creation activity.
    private void createAccount() {
        startActivity(new Intent(this, AccountActivity.class));
    }

    public void onAccountSelected(View v) {
        CheckedTextView accountView = (CheckedTextView) v.findViewById(R.id.chooser_account);
        accountView.setChecked(!accountView.isChecked());
    }

    public void onOK(View v) {
        LinearLayout accountsView = (LinearLayout) findViewById(R.id.chooser_accounts);
        ArrayList<String> selectedAccounts = new ArrayList<>();
        for (int i = 0; i < accountsView.getChildCount(); ++i) {
            CheckedTextView accountView =
                    (CheckedTextView) accountsView.getChildAt(i).findViewById(R.id.chooser_account);
            if (accountView.isChecked()) {
                selectedAccounts.add(accountView.getText().toString());
            }
        }
        if (selectedAccounts.isEmpty()) {
            Toast.makeText(this, "Must select an account.", Toast.LENGTH_LONG).show();
            return;
        }
        replyWithSuccess(selectedAccounts.toArray(new String[selectedAccounts.size()]));
    }

    public void onCancel(View v) {
        replyWithError("User canceled account selection.");
    }

    public void onAdd(View v) {
        createAccount();
    }

    private void replyWithSuccess(String[] accountNames) {
        Intent intent = new Intent();
        intent.putExtra(REPLY, accountNames);
        setResult(RESULT_OK, intent);
        finish();
    }

    private void replyWithError(String error) {
        android.util.Log.e(TAG, "Account choosing error: " + error);
        Intent intent = new Intent();
        intent.putExtra(ERROR, error);
        setResult(RESULT_CANCELED, intent);
        finish();
    }
}
