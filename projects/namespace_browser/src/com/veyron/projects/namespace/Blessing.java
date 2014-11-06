package com.veyron.projects.namespace;

import com.google.gson.Gson;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.RuntimeFactory;
import io.veyron.veyron.veyron2.VRuntime;
import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.security.WireBlessings;
import io.veyron.veyron.veyron2.vdl.JSONUtil;

import java.security.interfaces.ECPublicKey;

public class Blessing {
	private static final String BLESSING_PKG = "com.veyron.projects.accounts";
	private static final String BLESSING_ACTIVITY = "BlessingActivity";
	private static final String ACCOUNT_NAME_KEY = "ACCOUNT_NAME";
	private static final String BLESSEE_PUBKEY_KEY = "BLESSEE_PUBKEY";
	private static final String ERROR = "ERROR";
	private static final String REPLY = "REPLY";

	public static Intent createIntent(Context ctx, String accountName) {
		final VRuntime r = RuntimeFactory.initRuntime(ctx, new Options());
		final ECPublicKey key = r.getPrincipal().publicKey();
		final Intent intent = new Intent();
		intent.setComponent(new ComponentName(
				BLESSING_PKG, BLESSING_PKG + "." + BLESSING_ACTIVITY));
		intent.putExtra(ACCOUNT_NAME_KEY, accountName);
		intent.putExtra(BLESSEE_PUBKEY_KEY, key);
		return intent;
	}

	public static WireBlessings getBlessings(int resultCode, Intent data) throws VeyronException {
		final Gson gson = JSONUtil.getGsonBuilder().create();
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
			throw new VeyronException("Got multiple certificate chains: " + gson.toJson(wire));
		}
		if (wire.getCertificateChains().get(0).size() <= 0) {
			throw new VeyronException("Got empty certificate chain");
		}
		return wire;
	}
}
