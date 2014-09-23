package com.veyron.projects.accounts;

import com.google.common.reflect.TypeToken;

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
import android.preference.PreferenceManager;
import android.widget.Toast;

import io.veyron.veyron.veyron.services.identity.OAuthBlesser;
import io.veyron.veyron.veyron.services.identity.OAuthBlesserFactory;
import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.RuntimeFactory;
import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.security.wire.Certificate;
import io.veyron.veyron.veyron2.security.wire.ChainPublicID;
import io.veyron.veyron.veyron2.vdl.JSONUtil;

import java.io.IOException;

public class BlessingActivity extends AccountAuthenticatorActivity {
  public static final String TAG = "net.veyron";
  private static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
  private static final int REQUEST_CODE_USER_APPROVAL = 1001;

  private static final String OAUTH_PROFILE = "https://www.googleapis.com/auth/userinfo.email";
  private static final String OAUTH_SCOPE = "oauth2:" + OAUTH_PROFILE;

  private static final String PREF_VEYRON_IDENTITY_SERVICE = "pref_identity_service_name";
  private static final String DEFAULT_IDENTITY_SERVICE_NAME =
      "/proxy.envyor.com:8101/identity/veyron-test/google";
  
  String mAccountName = "", mAccountType = "";
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_blessing);   
    final Intent intent = AccountManager.newChooseAccountIntent(
        null, null, new String[]{"com.google" /* add Facebook */}, true, null, null, null, null);
    startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
  }
 
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_CODE_PICK_ACCOUNT) {
      if (resultCode != RESULT_OK) {
        replyWithError("User didn't pick account.");
        return;
      }
      mAccountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
      mAccountType = data.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
      getIdentity();
    } else if (requestCode == REQUEST_CODE_USER_APPROVAL) {
      if (resultCode != RESULT_OK) {
        replyWithError("User didn't give approve proposed permissions.");
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
    if (mAccountType == null || mAccountType.isEmpty()) {
      replyWithError("Empty account type.");
      return;
    }
    final Account[] accounts = AccountManager.get(this).getAccounts();
    Account account = null;
    for (int i = 0; i < accounts.length; i++) {
      if (accounts[i].name.equals(mAccountName) && accounts[i].type.equals(mAccountType)) {
        account = accounts[i];
      }
    }
    if (account == null) {
      replyWithError(String.format("Couldn't find account with name: %s and type: %s.",
          mAccountName, mAccountType));
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
        final Bundle bundle = result.getResult();
        final Intent launch = (Intent) bundle.get(AccountManager.KEY_INTENT);
        if (launch != null) {  // Needs user approval.
          // NOTE(spetrovic): The returned intent has the wrong flag value FLAG_ACTIVITY_NEW_TASK set,
          // which results in the launched intent replying immediately with RESULE_CANCELED.
          // Hence, we clear the flag here.
          startActivityForResult(launch, REQUEST_CODE_USER_APPROVAL);
          return;
        }
        final String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
        (new BlessingFetcher()).execute(token);
      } catch (AuthenticatorException e){
        replyWithError("Couldn't authorize: " + e.getMessage());
      } catch (OperationCanceledException e) {
        replyWithError("Authorization cancelled: " + e.getMessage());
      } catch (IOException e) {
        replyWithError("Unexpected error: " + e.getMessage());
      }
    }
  }
  
  private class BlessingFetcher extends AsyncTask<String, Void, ChainPublicID> {
    final ProgressDialog progressDialog = new ProgressDialog(BlessingActivity.this);
    String errorMsg = null;
    @Override
    protected void onPreExecute() {
      progressDialog.setMessage("Fetching Veyron Identity...");
      progressDialog.show();
    }
    @Override
    protected ChainPublicID doInBackground(String... tokens) {
      if (tokens.length != 1) {
        errorMsg = "Empty OAuth token.";
        return null;
      }
      RuntimeFactory.init(BlessingActivity.this, new Options());
      final String identityServiceName = PreferenceManager.getDefaultSharedPreferences(
          BlessingActivity.this).getString(
              PREF_VEYRON_IDENTITY_SERVICE, DEFAULT_IDENTITY_SERVICE_NAME);
      try {
        final OAuthBlesser blesser = OAuthBlesserFactory.bind(identityServiceName);
        final io.veyron.veyron.veyron2.vdl.Any reply = blesser.blessUsingAccessToken(null, tokens[0]);
        final ChainPublicID blessing = (ChainPublicID) reply.decode(new TypeToken<ChainPublicID>(){});
        if (blessing == null ||
            blessing.getCertificates() == null ||
            blessing.getCertificates().isEmpty()) {
          errorMsg = "Received empty blessing from Veyron identity servers.";
          return null;
        }
        return blessing;
      } catch (VeyronException e) {
        errorMsg = e.getMessage();
        return null;
      }
    }
    @Override
    protected void onPostExecute(ChainPublicID blessing) {
      progressDialog.dismiss();
      if (blessing == null) {  // Indicates an error
        replyWithError("Couldn't get identity from Veyron identity servers: " + errorMsg);
        return;
      }
      replyWithSuccess(blessing);
    }
  }

  private void replyWithError(String error) {
    android.util.Log.e(TAG, "Error creating account: " + error);
    setResult(RESULT_CANCELED);
    final String text = "Couldn't create account: " + error;
    final Toast toast = Toast.makeText(this, text, Toast.LENGTH_LONG);
    toast.show();
    finish();
  }
 
  private void replyWithSuccess(ChainPublicID id) {
    final String encoded = JSONUtil.getGsonBuilder().create().toJson(id);
    final String userName = userNameFromChain(id);

    final Account account = new Account(userName, getResources().getString(R.string.authenticator_account_type));
    final AccountManager am = AccountManager.get(this);
    am.addAccountExplicitly(account, null, null);
    am.setAuthToken(account, "ChainPublicID", encoded);
    setAccountAuthenticatorResult(new Intent().getExtras());
    setResult(RESULT_OK);
    final Toast toast = Toast.makeText(this, "Success.", Toast.LENGTH_SHORT);
    toast.show();
    finish();
  }
  
  private static String userNameFromChain(ChainPublicID id) {
    String ret = "";
    for (Certificate c : id.getCertificates()) {
      if (!ret.isEmpty()) {
        ret += "/";
      }
      ret += c.getName();
    }
    return ret;
  }
}