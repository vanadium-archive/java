package com.veyron.projects.accounts;

import com.google.common.reflect.TypeToken;
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
import android.os.Parcelable;
import android.view.View;
import android.widget.TextView;

import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.android.RuntimeFactory;
import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.security.Blessings;
import io.veyron.veyron.veyron2.security.Certificate;
import io.veyron.veyron.veyron2.security.Principal;
import io.veyron.veyron.veyron2.security.Security;
import io.veyron.veyron.veyron2.security.WireBlessings;
import io.veyron.veyron.veyron2.vdl.JSONUtil;

import java.io.IOException;
import java.security.interfaces.ECPublicKey;
import java.util.List;

public class BlessingActivity extends AccountAuthenticatorActivity {
	public static final String TAG = "com.veyron.projects.accounts";

	public static final String ACCOUNT_NAME_KEY = "ACCOUNT_NAME";
	public static final String BLESSEE_PUBKEY_KEY = "BLESSEE_PUBKEY";
	public static final String ERROR = "ERROR";
	public static final String REPLY = "REPLY";

	private static final String ACCOUNT_TYPE = "com.veyron";

	Account mAccount = null;
	String mBlesseeName = "";
	ECPublicKey mBlesseePubKey = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_blessing);
		try {
			RuntimeFactory.initRuntime(this, new Options());
		} catch (VeyronException e) {
			replyWithError("Couldn't initialize Veyron runtime.");
			return;
		}
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
					"Couldn't find account %s of type %s", accountName, ACCOUNT_TYPE));
			return;
		}
		((TextView)findViewById(R.id.text_account)).setText(accountName);

		// Decode the PublicKey to be blessed.
		mBlesseePubKey = (ECPublicKey) b.getSerializable(BLESSEE_PUBKEY_KEY);
		if (mBlesseePubKey == null) {
			replyWithError("Empty blessee public key.");
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
				final Bundle bundle = result.getResult();
				final String wireJson = bundle.getString(AccountManager.KEY_AUTHTOKEN);
				if (wireJson == null || wireJson.isEmpty()) {
					replyWithError("Empty auth token.");
					return;
				}
				bless(wireJson);
			} catch (AuthenticatorException e){
				replyWithError("Couldn't authorize: " + e.getMessage());
			} catch (OperationCanceledException e) {
				replyWithError("Authorization cancelled: " + e.getMessage());
			} catch (IOException e) {
				replyWithError("Unexpected error: " + e.getMessage());
			}
		}
	}

	private void bless(String wireJson) {
		final Gson gson = JSONUtil.getGsonBuilder().create();
		try {
			final WireBlessings wire = gson.fromJson(wireJson,
					new TypeToken<WireBlessings>(){}.getType());
			final Blessings with = Security.newBlessings(wire);
			final Principal principal = RuntimeFactory.defaultRuntime().getPrincipal();
			final Blessings retBlessings = principal.bless(mBlesseePubKey,
					with, mBlesseeName, Security.newUnconstrainedUseCaveat());
			if (retBlessings == null) {
				replyWithError("Got null blessings after bless().");
				return;
			}
			final WireBlessings retWire = retBlessings.wireFormat();
			if (retWire == null) {
				replyWithError("Got null wire blessings even though blessings are non-null");
				return;
			}
			if (retWire.getCertificateChains().size() <= 0) {
				replyWithError("Got empty certificate chains.");
				return;
			}
			if (retWire.getCertificateChains().size() > 1) {
				replyWithError("Expected single certificate chain, got: " + gson.toJson(retWire));
				return;
			}
			final List<Certificate> chain = retWire.getCertificateChains().get(0);
			if (chain == null || chain.size() <= 0) {
				replyWithError("Empty certificate chain");
				return;
			}
			replyWithSuccess(retWire);
		} catch (JsonSyntaxException e) {
			replyWithError("Couldn't decode auth token: " + e.getMessage());
		} catch (VeyronException e) {
			replyWithError("Couldn't bless: " + e.getMessage());
		}
	}

	private void replyWithSuccess(WireBlessings wire) {
		final Intent intent = new Intent();
		intent.putExtra(REPLY, (Parcelable)wire);
		setResult(RESULT_OK, intent);
		finish();
	}

	private void replyWithError(String error) {
		final Intent intent = new Intent();
		intent.putExtra(ERROR, error);
		setResult(RESULT_CANCELED, intent);
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