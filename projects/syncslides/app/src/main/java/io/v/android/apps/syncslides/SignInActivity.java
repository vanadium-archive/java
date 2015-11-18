// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Signs in the user into one of his Gmail accounts.
 */
public class SignInActivity extends AppCompatActivity {
    private static final String TAG = "SignInActivity";

    private static final String PREF_USER_ACCOUNT_NAME = "user_account";
    private static final String PREF_USER_NAME_FROM_CONTACTS = "user_name_from_contacts";
    private static final String PREF_USER_NAME_FROM_PROFILE = "user_name_from_profile";
    private static final String PREF_USER_PROFILE_JSON = "user_profile";

    private static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    private static final int REQUEST_CODE_FETCH_USER_PROFILE_APPROVAL = 1001;
    private static final int REQUEST_CODE_READ_CONTACTS = 100;

    private static final String OAUTH_PROFILE = "email";
    private static final String OAUTH_SCOPE = "oauth2:" + OAUTH_PROFILE;
    private static final String OAUTH_USERINFO_URL =
            "https://www.googleapis.com/oauth2/v2/userinfo";

    // NOTE(spetrovic): For demo purposes, fetching user profile is too risky as it requires
    // internet access.  So we disable it for now.
    private static final boolean FETCH_PROFILE = false;

    private SharedPreferences mPrefs;
    private String mAccountName;
    private ProgressDialog mProgressDialog;

    /**
     * Returns the best-effort email of the signed-in user.
     */
    public static String getUserEmail(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        return prefs.getString(PREF_USER_ACCOUNT_NAME, "");
    }

    /**
     * Returns the best-effort full name of the signed-in user.
     */
    public static String getUserName(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        // First try to read the user name we obtained from profile, as it's most accurate.
        if (prefs.contains(PREF_USER_NAME_FROM_PROFILE)) {
            return prefs.getString(PREF_USER_NAME_FROM_PROFILE, "Anonymous User");
        }
        return prefs.getString(PREF_USER_NAME_FROM_CONTACTS, "Anonymous User");
    }

    /**
     * Returns the Google profile information of the signed-in user, or {@code null} if the
     * profile information couldn't be retrieved.
     */
    public static JSONObject getUserProfile(Context ctx) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        String userProfileJsonStr = prefs.getString(PREF_USER_PROFILE_JSON, "");
        if (!userProfileJsonStr.isEmpty()) {
            try {
                return new JSONObject(userProfileJsonStr);
            } catch (JSONException e) {
                Log.e(TAG, "Couldn't parse user profile data: " + userProfileJsonStr);
            }
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mAccountName = mPrefs.getString(SignInActivity.PREF_USER_ACCOUNT_NAME, "");
        mProgressDialog = new ProgressDialog(this);
        if (mAccountName.isEmpty()) {
            mProgressDialog.setMessage("Signing in...");
            mProgressDialog.show();
            pickAccount();
        } else {
            finishActivity();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PICK_ACCOUNT: {
                if (resultCode != RESULT_OK) {
                    Toast.makeText(this, "Must pick account", Toast.LENGTH_LONG).show();
                    pickAccount();
                    break;
                }
                pickAccountDone(data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
                break;
            }
            case REQUEST_CODE_FETCH_USER_PROFILE_APPROVAL:
                if (resultCode != RESULT_OK) {
                    Log.e(TAG, "User didn't approve oauth2 request");
                    break;
                }
                fetchUserProfile();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permission[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_READ_CONTACTS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestContactsDone();
                    return;
                }
                Log.e(TAG, "User didn't approve read contacts");
                fetchUserProfile();
            }
        }
    }

    private void pickAccount() {
        Intent chooseIntent = AccountManager.newChooseAccountIntent(
                null, null, new String[]{"com.google"}, false, null, null, null, null);
        startActivityForResult(chooseIntent, REQUEST_CODE_PICK_ACCOUNT);
    }

    private void pickAccountDone(String accountName) {
        mAccountName = accountName;
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_USER_ACCOUNT_NAME, accountName);
        editor.commit();

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS);
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            requestContactsDone();
            return;
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.GET_ACCOUNTS}, REQUEST_CODE_READ_CONTACTS);
    }

    private void requestContactsDone() {
        fetchUserNameFromContacts();

         fetchUserProfile();
    }

    private void fetchUserNameFromContacts() {
        // Get the user's full name from Contacts.
        Cursor c = getContentResolver().query(ContactsContract.Profile.CONTENT_URI,
                null, null, null, null);
        String[] columnNames = c.getColumnNames();
        String userName = "Anonymous User";
        while (c.moveToNext()) {
            for (int j = 0; j < columnNames.length; j++) {
                String columnName = columnNames[j];
                if (!columnName.equals(ContactsContract.Contacts.DISPLAY_NAME)) {
                    continue;
                }
                userName = c.getString(c.getColumnIndex(columnName));
            }
        }
        c.close();
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putString(PREF_USER_NAME_FROM_CONTACTS, userName);
        editor.commit();
    }

    private void fetchUserProfile() {
        if (!FETCH_PROFILE) {
            fetchUserProfileDone(null);
            return;
        }
        AccountManager manager = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
        Account[] accounts = manager.getAccountsByType("com.google");
        Account account = null;
        for (int i = 0; i < accounts.length; i++) {
            if (accounts[i].name.equals(mAccountName)) {
                account = accounts[i];
                break;
            }
        }
        if (account == null) {
            Log.e(TAG, "Couldn't find Google account with name: " + mAccountName);
            pickAccount();
            return;
        }
        manager.getAuthToken(account,
                OAUTH_SCOPE,
                new Bundle(),
                false,
                new OnTokenAcquired(),
                new Handler(new Handler.Callback() {
                    @Override
                    public boolean handleMessage(Message msg) {
                        Log.e(TAG, "Error getting auth token: " + msg.toString());
                        fetchUserProfileDone(null);
                        return true;
                    }
                }));
    }

    private void fetchUserProfileDone(JSONObject userProfile) {
        if (userProfile != null) {
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putString(PREF_USER_PROFILE_JSON, userProfile.toString());
            try {
                if (userProfile.has("name") && !userProfile.getString("name").isEmpty()) {
                    editor.putString(PREF_USER_NAME_FROM_PROFILE, userProfile.getString("name"));
                }
            } catch (JSONException e) {
                Log.e(TAG, "Couldn't read user name from user profile: " + e.getMessage());
            }
            editor.commit();
        }
        finishActivity();
    }


    private void finishActivity() {
        mProgressDialog.dismiss();
        startActivity(new Intent(this, DeckChooserActivity.class));
        finish();
    }

    private class OnTokenAcquired implements AccountManagerCallback<Bundle> {
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
                    startActivityForResult(launch, REQUEST_CODE_FETCH_USER_PROFILE_APPROVAL);
                    return;
                }
                String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                new ProfileInfoFetcher().execute(token);
            } catch (AuthenticatorException e) {
                Log.e(TAG, "Couldn't authorize: " + e.getMessage());
                fetchUserProfileDone(null);
            } catch (OperationCanceledException e) {
                Log.e(TAG, "Authorization cancelled: " + e.getMessage());
                fetchUserProfileDone(null);
            } catch (IOException e) {
                Log.e(TAG, "Unexpected error: " + e.getMessage());
                fetchUserProfileDone(null);
            }
        }
    }

    private class ProfileInfoFetcher extends AsyncTask<String, Void, JSONObject> {
        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                URL url = new URL(OAUTH_USERINFO_URL + "?access_token=" + params[0]);
                return new JSONObject(CharStreams.toString(
                        new InputStreamReader(url.openConnection().getInputStream(),
                                Charsets.US_ASCII)));
            } catch (MalformedURLException e) {
                Log.e(TAG, "Error fetching user's profile info" + e.getMessage());
            } catch (JSONException e) {
                Log.e(TAG, "Error fetching user's profile info" + e.getMessage());
            } catch (IOException e) {
                Log.e(TAG, "Error fetching user's profile info" + e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject userProfile) {
            fetchUserProfileDone(userProfile);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_sign_in, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
