package io.veyron.veyron.veyron2.security;

import io.veyron.veyron.veyron2.ipc.VeyronException;

import java.security.interfaces.ECPublicKey;

class BlessingStoreImpl implements BlessingStore {
	private static final String TAG = "Veyron runtime";

	private final long nativePtr;

	private native Blessings nativeSet(
		long nativePtr, Blessings blessings, BlessingPattern forPeers) throws VeyronException;
	private native Blessings nativeForPeer(long nativePtr, String[] peerBlessings)
		throws VeyronException;
	private native void nativeSetDefaultBlessings(long nativePtr, Blessings blessings)
		throws VeyronException;
	private native Blessings nativeDefaultBlessings(long nativePtr) throws VeyronException;
	private native ECPublicKey nativePublicKey(long nativePtr) throws VeyronException;
	private native String nativeDebugString(long nativePtr);
	private native String nativeToString(long nativePtr);
	private native void nativeFinalize(long nativePtr);

	private BlessingStoreImpl(long nativePtr) {
		this.nativePtr = nativePtr;
	}

	@Override
	public Blessings set(Blessings blessings, BlessingPattern forPeers) throws VeyronException {
		return nativeSet(this.nativePtr, blessings, forPeers);
	}
	@Override
	public Blessings forPeer(String... peerBlessings) {
		try {
			return nativeForPeer(this.nativePtr, peerBlessings);
		} catch (VeyronException e) {
			android.util.Log.e(TAG, "Couldn't get blessings for peers: " + e.getMessage());
			return null;
		}
	}
	@Override
	public void setDefaultBlessings(Blessings blessings) throws VeyronException {
		nativeSetDefaultBlessings(this.nativePtr, blessings);
	}
	@Override
	public Blessings defaultBlessings() {
		try {
			return nativeDefaultBlessings(this.nativePtr);
		} catch (VeyronException e) {
			android.util.Log.e(TAG, "Couldn't get default blessings: " + e.getMessage());
			return null;
		}
	}
	@Override
	public ECPublicKey publicKey() {
		try {
			return nativePublicKey(this.nativePtr);
		} catch (VeyronException e) {
			android.util.Log.e(TAG, "Couldn't get public key: " + e.getMessage());
			return null;
		}
	}
	@Override
	public String debugString() {
		return nativeDebugString(this.nativePtr);
	}
	@Override
	public String toString() {
		return nativeToString(this.nativePtr);
	}
	@Override
	public void finalize() {
		nativeFinalize(this.nativePtr);
	}
}