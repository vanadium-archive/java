package io.veyron.veyron.veyron2.security;

import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.vdl.JSONUtil;

import java.security.interfaces.ECPublicKey;

class BlessingsImpl extends Blessings {
	private static final String TAG = "Veyron runtime";

	private static native long nativeCreate(WireBlessings wire) throws VeyronException;

	static BlessingsImpl create(WireBlessings wire) throws VeyronException {
		final long nativePtr = nativeCreate(wire);
		return new BlessingsImpl(nativePtr, Util.chainsToArray(wire.getCertificateChains()));
	}

	private final long nativePtr;
	private final Certificate[][] chains;

	private native String[] nativeForContext(long nativePtr, Context context)
		throws VeyronException;
	private native ECPublicKey nativePublicKey(long nativePtr) throws VeyronException;
	private native void nativeFinalize(long nativePtr);

	private BlessingsImpl(long nativePtr, Certificate[][] chains) {
		this.nativePtr = nativePtr;
		this.chains = chains;
	}

	@Override
	public String[] forContext(Context context) {
		try {
			return nativeForContext(this.nativePtr, context);
		} catch (VeyronException e) {
			android.util.Log.e(TAG, "Couldn't get blessings for context: " + e.getMessage());
			return null;
		}
	}

	@Override
	public ECPublicKey publicKey() {
		try {
			return nativePublicKey(this.nativePtr);
		} catch (VeyronException e) {
			android.util.Log.e(TAG, "Coudln't get public key: " + e.getMessage());
			return null;
		}
	}

	@Override
	Certificate[][] certificateChains() {
		return this.chains;
	}
	@Override
	public String toString() {
		return JSONUtil.getGsonBuilder().create().toJson(certificateChains());
	}
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}

}