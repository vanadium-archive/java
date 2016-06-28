// Copyright 2016 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.syncbase.android;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class LoginFragment extends Fragment {
    private static final String TAG = "LoginFragment";

    private static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    private static final int REQUEST_CODE_USER_APPROVAL = 1001;
    private static final String OAUTH_SCOPE =
            "oauth2:https://www.googleapis.com/auth/userinfo.email";
    private static final String ACCOUNT_TYPE = "com.google";

    private String mGoogleAccount = "";
    private TokenReceiver tokenReceiver;

    public LoginFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Note: newChooseAccountIntent is deprecated, but the API level we're targeting is < 23,
        // which is when the replacement is introduced. If we do upgrade to API v23, just remove the
        // "false" parameter.
        Intent accountIntent = AccountManager.newChooseAccountIntent(
                null, null, new String[]{ACCOUNT_TYPE}, false, null, null, null, null);
        startActivityForResult(accountIntent, REQUEST_CODE_PICK_ACCOUNT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PICK_ACCOUNT:
                if (resultCode != Activity.RESULT_OK) {
                    Log.e(TAG, "User didn't pick account: " + resultCode);
                    return;
                }
                mGoogleAccount = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                Log.i(TAG, "Account name: " + mGoogleAccount);
                getAccessToken();
                break;
            case REQUEST_CODE_USER_APPROVAL:
                if (resultCode != Activity.RESULT_OK) {
                    Log.e(TAG, "User didn't give proposed permissions: " + resultCode);
                    return;
                }
                getAccessToken();
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    // Call this immediately after fragment creation.
    public void setTokenReceiver(TokenReceiver receiver) {
        tokenReceiver = receiver;
    }

    public interface TokenReceiver {
        void receiveToken(String token);
    }

    private void getAccessToken() {
        Account account = new Account(mGoogleAccount, ACCOUNT_TYPE);
        AccountManager.get(this.getActivity()).getAuthToken(
                account,
                OAUTH_SCOPE,
                new Bundle(),
                false,
                new OnTokenAcquired(this),
                new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        Log.e(TAG, "Error getting auth token: " + msg.toString());
                        return true;
                    }
                }));
    }

    private class OnTokenAcquired implements AccountManagerCallback<Bundle> {
        private final Fragment fragment;

        OnTokenAcquired(Fragment fragment) {
            this.fragment = fragment;
        }

        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            try {
                Bundle bundle = result.getResult();
                Intent launch = (Intent) bundle.get(AccountManager.KEY_INTENT);
                if (launch != null) {
                    launch.setFlags(0);
                    startActivityForResult(launch, REQUEST_CODE_USER_APPROVAL);
                    return;
                }
                String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                Log.i(TAG, "token: " + token);
                tokenReceiver.receiveToken(token);
            } catch (Exception e) {
                Log.e(TAG, "onTokenAcquired errored", e);
            }

            // The token has been acquired (or failed). Remove the fragment.
            FragmentTransaction transaction = getActivity().getFragmentManager().beginTransaction();
            transaction.remove(fragment);
            transaction.commit();
        }
    }
}
