package io.veyron.veyron.veyron2.security;

import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.vdl.JSONUtil;

import java.security.interfaces.ECPublicKey;

class BlessingsImpl extends Blessings {
	private static final String TAG = "Veyron runtime";

	private static native long nativeCreate(WireBlessings wire) throws VeyronException;

	static BlessingsImpl create(WireBlessings wire) throws VeyronException {
		final long nativePtr = nativeCreate(wire);
		return new BlessingsImpl(nativePtr, wire);
	}

	private final long nativePtr;
	private final WireBlessings wire;  // non-null

	private native String[] nativeForContext(long nativePtr, Context context)
		throws VeyronException;
	private native ECPublicKey nativePublicKey(long nativePtr) throws VeyronException;
	private native void nativeFinalize(long nativePtr);

	private BlessingsImpl(long nativePtr, WireBlessings wire) {
		this.nativePtr = nativePtr;
		this.wire = wire;
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
	public WireBlessings wireFormat() {
		return this.wire;
	}
	@Override
	void implementationsOnlyInThisPackage() {
	}
	@Override
	public String toString() {
		return JSONUtil.getGsonBuilder().create().toJson(this.wire);
	}
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}