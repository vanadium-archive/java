package com.veyron.projects.namespace;

import com.google.gson.Gson;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.RuntimeFactory;
import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.security.PublicID;
import io.veyron.veyron.veyron2.security.wire.ChainPublicID;
import io.veyron.veyron.veyron2.vdl.JSONUtil;

public class Blessing {
	private static final String BLESSING_PKG = "com.veyron.projects.accounts";
	private static final String BLESSING_ACTIVITY = "BlessingActivity";
	private static final String ACCOUNT_NAME_KEY = "ACCOUNT_NAME";
	private static final String BLESSEE_ID_KEY = "BLESSEE_ID";
	private static final String ERROR = "ERROR";
	private static final String REPLY = "REPLY";

	public static Intent createIntent(Context ctx, String accountName) throws VeyronException {
		final Gson gson = JSONUtil.getGsonBuilder().create();
		final io.veyron.veyron.veyron2.Runtime r = RuntimeFactory.init(ctx, new Options());
		final PublicID id = r.getIdentity().publicID();
		final ChainPublicID[] chains = id.encode();
		if (chains.length != 1) {
			throw new VeyronException("Chain not of length 1: " + gson.toJson(chains));
		}
		final Intent intent = new Intent();
		intent.setComponent(new ComponentName(
				BLESSING_PKG, BLESSING_PKG + "." + BLESSING_ACTIVITY));
		intent.putExtra(ACCOUNT_NAME_KEY, accountName);
		intent.putExtra(BLESSEE_ID_KEY, gson.toJson(chains[0]));
		return intent;
	}

	public static String getReply(int resultCode, Intent data) throws VeyronException {
		if (data == null) {
			throw new VeyronException("NULL blessing response");
		}
		if (resultCode == Activity.RESULT_OK) {
			return data.getStringExtra(REPLY);
		}
		throw new VeyronException("Error getting blessing: " + data.getStringExtra(ERROR));
	}
}
