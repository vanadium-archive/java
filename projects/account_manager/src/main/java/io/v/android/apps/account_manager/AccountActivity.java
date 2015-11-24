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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.widget.Toast;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;
import java.util.List;

import io.v.android.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.BlessingStore;
import io.v.v23.security.Blessings;
import io.v.v23.security.Caveat;
import io.v.v23.security.CryptoUtil;
import io.v.v23.security.VPrincipal;
import io.v.v23.security.VSecurity;
import io.v.v23.verror.VException;
import io.v.x.ref.services.identity.OAuthBlesserClient;
import io.v.x.ref.services.identity.OAuthBlesserClientFactory;

/**
 * Creates a new Vanadium account, using the Google accounts present on the device.
 */
// TODO: Change to BlessingCreationActivity.
public class AccountActivity extends AccountAuthenticatorActivity {
    public static final String TAG = "AccountActivity";

    private static final int REQUEST_CODE_PICK_ACCOUNTS = 1000;
    private static final int REQUEST_CODE_USER_APPROVAL = 1001;

    private static final String OAUTH_PROFILE = "https://www.googleapis.com/auth/userinfo.email";
    private static final String OAUTH_SCOPE = "oauth2:" + OAUTH_PROFILE;

    public static final String GOOGLE_ACCOUNT = "GOOGLE_ACCOUNT";

    private VContext mBaseContext = null;
    private String mAccountName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        mBaseContext = V.init(this);

        Intent intent = getIntent();
        if (intent == null) {
            replyWithError("Intent not found.");
            return;
        }
        // See if the caller wants to use a specific Google account to create the Vanadium account.
        // If null or empty string is passed, the user will be prompted to choose the Google
        // account to use.
        mAccountName = intent.getStringExtra(GOOGLE_ACCOUNT);
        if (mAccountName != null && !mAccountName.isEmpty()) {
            getIdentity();
            return;
        }
        Intent chooseIntent = AccountManager.newChooseAccountIntent(
                null, null, new String[]{"com.google"}, false, null, null, null, null);
        startActivityForResult(chooseIntent, REQUEST_CODE_PICK_ACCOUNTS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_ACCOUNTS) {
            if (resultCode != RESULT_OK) {
                replyWithError("User didn't pick account.");
                return;
            }
            mAccountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            getIdentity();
        } else if (requestCode == REQUEST_CODE_USER_APPROVAL) {
            if (resultCode != RESULT_OK) {
                replyWithError("User didn't give proposed permissions.");
                return;
            }
            getIdentity();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private void getIdentity() {
        if (mAccountName == null || mAccountName.isEmpty()) {
            replyWithError("Empty account name.");
            return;
        }
        Account[] accounts = AccountManager.get(this).getAccountsByType("com.google");
        Account account = null;
        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i].name.equals(mAccountName)) {
                account = accounts[i];
            }
        }
        if (account == null) {
            replyWithError("Couldn't find Google account with name: " + mAccountName);
            return;
        }
        AccountManager.get(this).getAuthToken(
                account,
                OAUTH_SCOPE,
                new Bundle(),
                false,
                new OnTokenAcquired(),
                new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        replyWithError("Error getting auth token: " + msg.toString());
                        return true;
                    }
                }));
    }
    class OnTokenAcquired implements AccountManagerCallback<Bundle> {
        @Override
        public void run(AccountManagerFuture<Bundle> result) {
            try {
                Bundle bundle = result.getResult();
                Intent launch = (Intent) bundle.get(AccountManager.KEY_INTENT);
                if (launch != null) {  // Needs user approval.
                    // NOTE(spetrovic): The returned intent has the wrong flag value
                    // FLAG_ACTIVITY_NEW_TASK set, which results in the launched intent replying
                    // immediately with RESULT_CANCELED.  Hence, we clear the flag here.
                    launch.setFlags(0);
                    startActivityForResult(launch, REQUEST_CODE_USER_APPROVAL);
                    return;
                }
                String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                (new BlessingFetcher()).execute(token);
            } catch (AuthenticatorException e) {
                replyWithError("Couldn't authorize: " + e.getMessage());
            } catch (OperationCanceledException e) {
                replyWithError("Authorization cancelled: " + e.getMessage());
            } catch (IOException e) {
                replyWithError("Unexpected error: " + e.getMessage());
            }
        }
    }
    private class BlessingFetcher extends AsyncTask<String, Void, Blessings> {
        ProgressDialog progressDialog = new ProgressDialog(AccountActivity.this);
        String errorMsg = null;
        @Override
        protected void onPreExecute() {
            progressDialog.setMessage("Fetching Vanadium Identity...");
            progressDialog.show();
        }
        @Override
        protected Blessings doInBackground(String... tokens) {
            if (tokens.length != 1) {
                errorMsg = "Empty OAuth token.";
                return null;
            }
            try {
                URL url = new URL("https://dev.v.io/auth/blessing-root");
                JSONObject object = new JSONObject(CharStreams.toString(
                        new InputStreamReader(url.openConnection().getInputStream(),
                                Charsets.US_ASCII)));
                String publicKey = object.get("publicKey").toString();
                byte[] base64DecodedKey = Base64.decode(
                        publicKey.getBytes(), Base64.URL_SAFE);
                ECPublicKey ecPublicKey = CryptoUtil.decodeECPublicKey(base64DecodedKey);
                JSONArray namesArray = (JSONArray) object.get("names");
                for (int i = 0; i < namesArray.length(); i++) {
                    String name = namesArray.getString(i);
                    V.getPrincipal(mBaseContext).roots()
                            .add(ecPublicKey, new BlessingPattern(name));
                }
                OAuthBlesserClient blesser =
                        OAuthBlesserClientFactory.getOAuthBlesserClient("identity/dev.v.io:u/google");
                VContext ctx = mBaseContext.withTimeout(new Duration(20000));  // 20s
                OAuthBlesserClient.BlessUsingAccessTokenWithCaveatsOut reply =
                        blesser.blessUsingAccessTokenWithCaveats(ctx, tokens[0],
                                ImmutableList.<Caveat>of(VSecurity.newUnconstrainedUseCaveat()));
                Blessings blessing = reply.blessing;
                if (blessing == null || blessing.getCertificateChains() == null ||
                        blessing.getCertificateChains().size() <= 0) {
                    errorMsg = "Received empty blessing from Vanadium identity servers.";
                    return null;
                }
                if (blessing.getCertificateChains().size() > 1) {
                    errorMsg = "Received more than one blessing from Vanadium identity servers.";
                    return null;
                }
                return blessing;
            } catch (VException e) {
                errorMsg = e.getMessage();
                return null;
            } catch (MalformedURLException e) {
                errorMsg = e.getMessage();
                return null;
            } catch (JSONException e) {
                errorMsg = e.getMessage();
                return null;
            } catch (IOException e) {
                errorMsg = e.getMessage();
                return null;
            }
        }
        @Override
        protected void onPostExecute(Blessings blessing) {
            progressDialog.dismiss();
            if (blessing == null) {  // Indicates an error
                replyWithError("Couldn't get identity from Vanadium identity servers: " + errorMsg);
                return;
            }
            replyWithSuccess(blessing);
        }
    }
    private void replyWithError(String error) {
        android.util.Log.e(TAG, "Error creating account: " + error);
        setResult(RESULT_CANCELED);
        String text = "Couldn't create blessing: " + error;
        Toast.makeText(this, text, Toast.LENGTH_LONG).show();
        finish();
    }
    private void replyWithSuccess(Blessings blessing) {
        enforceAccountExists();
        // Store the obtained blessing from identity server.
        try {
            VPrincipal principal = V.getPrincipal(mBaseContext);
            BlessingStore blessingStore = principal.blessingStore();
            blessingStore.set(blessing, new BlessingPattern(blessing.toString()));
        } catch (VException e) {
            replyWithError("Couldn't store obtained blessing: " + e.getMessage());
        }
        setResult(RESULT_OK);
        Toast.makeText(this, "Success.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void enforceAccountExists() {
        if (AccountManager.get(this).getAccountsByType(Constants.ACCOUNT_TYPE).length <= 0){
            String name = "Vanadium";
            Account account = new Account(name, getResources().getString(
                    R.string.authenticator_account_type));
            AccountManager am = AccountManager.get(this);
            am.addAccountExplicitly(account, null, null);
        }
    }
}
