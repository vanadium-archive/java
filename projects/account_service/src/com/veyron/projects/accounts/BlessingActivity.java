package com.veyron.projects.accounts;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

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
import android.widget.TextView;

import org.joda.time.Duration;

import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.RuntimeFactory;
import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.security.PrivateID;
import io.veyron.veyron.veyron2.security.PublicID;
import io.veyron.veyron.veyron2.security.wire.ChainPublicID;
import io.veyron.veyron.veyron2.security.wire.ChainPublicIDImpl;
import io.veyron.veyron.veyron2.vdl.JSONUtil;

import java.io.IOException;

public class BlessingActivity extends AccountAuthenticatorActivity {
	public static final String TAG = "com.veyron.projects.accounts";

	public static final String ACCOUNT_NAME_KEY = "ACCOUNT_NAME";
	public static final String BLESSEE_ID_KEY = "BLESSEE_ID";
	public static final String ERROR = "ERROR";
	public static final String REPLY = "REPLY";

	private static final String ACCOUNT_TYPE = "com.veyron";

	Account mAccount = null;
	String mBlesseeName = "";
	PublicID mBlesseeID = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_blessing);
		RuntimeFactory.init(this, new Options());

		final Intent intent = getIntent();
		if (intent == null || intent.getExtras() == null) {
			replyWithError("No extras provided.");
			return;
		}
		mBlesseeName = getCallingActivity().getClassName();
		if (mBlesseeName == null || mBlesseeName.isEmpty()) {
			replyWithError("Empty blesee name.");
			return;
		}
		((TextView)findViewById(R.id.text_application)).setText(mBlesseeName);

		// Find the Veyron account with a given name.
		final Bundle b = getIntent().getExtras();
		final String accountName = b.getString(ACCOUNT_NAME_KEY);
		if (accountName == null || accountName.isEmpty()) {
			replyWithError("Empty account name.");
			return;
		}
		mAccount = findAccount(accountName);
		if (mAccount == null) {
			replyWithError(String.format(
					"Couldn't find account %q of type %q", accountName, ACCOUNT_TYPE));
			return;
		}
		((TextView)findViewById(R.id.text_account)).setText(accountName);

		// Decode the PublicID to be blessed.
		final String blesseeIDStr = b.getString(BLESSEE_ID_KEY);
		if (blesseeIDStr == null || blesseeIDStr.isEmpty()) {
			replyWithError("Empty blessee chain id.");
			return;
		}

		try {
			mBlesseeID = new ChainPublicIDImpl(blesseeIDStr);
		} catch (VeyronException e) {
			replyWithError("Couldn't decode blessee id: " + e.getMessage());
			return;
		}
	}

	public void onAccept(@SuppressWarnings("unused") View view) {
		blessAccount();
	}

	public void onDeny(@SuppressWarnings("unused") View view) {
		replyWithError("User denied blessing request.");
	}

	private void blessAccount() {
		AccountManager.get(this).getAuthToken(
				mAccount, "ChainPublicID", null, this, new OnTokenAcquired(),
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
				final Bundle bundle = result.getResult();
				final String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
				if (token == null || token.isEmpty()) {
					replyWithError("Empty auth token.");
					return;
				}
				blessToken(token);
			} catch (AuthenticatorException e){
				replyWithError("Couldn't authorize: " + e.getMessage());
			} catch (OperationCanceledException e) {
				replyWithError("Authorization cancelled: " + e.getMessage());
			} catch (IOException e) {
				replyWithError("Unexpected error: " + e.getMessage());
			}
		}
	}

	private void blessToken(String token) {
		try {
			final PublicID pubID = new ChainPublicIDImpl(token);
			final PrivateID privID = RuntimeFactory.init(this, new Options()).getIdentity();
			final PrivateID derivedPrivID = privID.derive(pubID);
			final PublicID blessedID =
					derivedPrivID.bless(mBlesseeID, mBlesseeName, Duration.standardDays(365));
			final ChainPublicID[] chain = blessedID.encode();
			if (chain.length > 1) {
				final Gson gson = JSONUtil.getGsonBuilder().create();
				replyWithError("Expected single identity chain, got: " + gson.toJson(chain));
				return;
			}
			replyWithSuccess(chain[0]);
		} catch (JsonSyntaxException e) {
			replyWithError("Couldn't decode auth token: " + e.getMessage());
		} catch (VeyronException e) {
			replyWithError("Couldn't bless: " + e.getMessage());
		}
	}

	private void replyWithSuccess(ChainPublicID id) {
		final String encoded = JSONUtil.getGsonBuilder().create().toJson(id);
		final Intent intent = new Intent();
		intent.putExtra(REPLY, encoded);
		setResult(RESULT_OK, intent);
		finish();
	}

	private void replyWithError(String error) {
		final Intent intent = new Intent();
		intent.putExtra(ERROR, error);
		setResult(RESULT_CANCELED);
		finish();
	}

	private Account findAccount(String accountName) {
		final Account[] accounts = AccountManager.get(this).getAccounts();
		for (Account account : accounts) {
			if (account.type.equals(ACCOUNT_TYPE) && account.name.equals(accountName)) {
				return account;
			}
		}
		return null;
	}
}