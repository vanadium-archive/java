package com.veyron.projects.namespace;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.android.VRuntime;
import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.security.WireBlessings;

import java.security.interfaces.ECPublicKey;

public class Blessing {
	private static final String BLESSING_PKG = "com.veyron.projects.accounts";
	private static final String BLESSING_ACTIVITY = "BlessingActivity";
	private static final String ACCOUNT_NAME_KEY = "ACCOUNT_NAME";
	private static final String BLESSEE_PUBKEY_KEY = "BLESSEE_PUBKEY";
	private static final String ERROR = "ERROR";
	private static final String REPLY = "REPLY";

	public static Intent createIntent(Context ctx, String accountName) {
	    VRuntime.init(ctx, new Options());
	    final ECPublicKey key = VRuntime.getPrincipal().publicKey();
	    final Intent intent = new Intent();
	    intent.setComponent(new ComponentName(
	            BLESSING_PKG, BLESSING_PKG + "." + BLESSING_ACTIVITY));
	    intent.putExtra(ACCOUNT_NAME_KEY, accountName);
	    intent.putExtra(BLESSEE_PUBKEY_KEY, key);
	    return intent;
	}

	public static WireBlessings getBlessings(int resultCode, Intent data) throws VeyronException {
		if (data == null) {
			throw new VeyronException("NULL blessing response");
		}
		if (resultCode != Activity.RESULT_OK) {
			throw new VeyronException("Error getting blessing: " + data.getStringExtra(ERROR));
		}
		final WireBlessings wire = (WireBlessings) data.getParcelableExtra(REPLY);
		if (wire == null) {
			throw new VeyronException("Got null blessings.");
		}
		if (wire.getCertificateChains().size() <= 0) {
			throw new VeyronException("Got empty blessings.");
		}
		if (wire.getCertificateChains().size() > 1) {
			throw new VeyronException("Got multiple certificate chains: " + wire.toString());
		}
		if (wire.getCertificateChains().get(0).size() <= 0) {
			throw new VeyronException("Got empty certificate chain");
		}
		return wire;
	}
}
