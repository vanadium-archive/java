package io.veyron.veyron.veyron2.security;

import io.veyron.veyron.veyron2.VeyronException;

import java.security.interfaces.ECPublicKey;

public class PrincipalImpl implements Principal {
	private static final String TAG = "Veyron runtime";

	private final long nativePtr;
	private final Signer signer;
	private final BlessingStore store;
	private final BlessingRoots roots;

	private static native PrincipalImpl nativeCreate() throws VeyronException;
	private static native PrincipalImpl nativeCreateForSigner(Signer signer) throws VeyronException;
	private static native PrincipalImpl nativeCreateForAll(Signer signer, BlessingStore store,
		BlessingRoots roots) throws VeyronException;
	private static native PrincipalImpl nativeCreatePersistent(String passphrase, String dir)
			throws VeyronException;
	private static native PrincipalImpl nativeCreatePersistentForSigner(Signer signer, String dir)
			throws VeyronException;

	static PrincipalImpl create() throws VeyronException {
		return nativeCreate();
	}
	static PrincipalImpl create(Signer signer) throws VeyronException {
		return nativeCreateForSigner(signer);
	}
	static PrincipalImpl create(Signer signer, BlessingStore store, BlessingRoots roots)
		throws VeyronException {
		return nativeCreateForAll(signer, store, roots);
	}
	static PrincipalImpl createPersistent(String passphrase, String dir) throws VeyronException {
		return nativeCreatePersistent(passphrase, dir);
	}
	static PrincipalImpl createPersistent(Signer signer, String dir) throws VeyronException {
		return nativeCreatePersistentForSigner(signer, dir);
	}

	private native Blessings nativeBless(long nativePtr, ECPublicKey key, Blessings with,
		String extension, Caveat caveat, Caveat[] additionalCaveats) throws VeyronException;
	private native Blessings nativeBlessSelf(long nativePtr, String name, Caveat[] caveats)
			throws VeyronException;
	private native Signature nativeSign(long nativePtr, byte[] message) throws VeyronException;
	private native ECPublicKey nativePublicKey(long nativePtr) throws VeyronException;
	private native Blessings[] nativeBlessingsByName(long nativePtr, BlessingPattern name)
			throws VeyronException;
	private native String[] nativeBlessingsInfo(long nativePtr, Blessings blessings)
			throws VeyronException;
	private native BlessingStore nativeBlessingStore(long nativePtr) throws VeyronException;
	private native BlessingRoots nativeRoots(long nativePtr) throws VeyronException;
	private native void nativeAddToRoots(long nativePtr, Blessings blessings)
			throws VeyronException;
	private native void nativeFinalize(long nativePtr);

	private PrincipalImpl(long nativePtr, Signer signer, BlessingStore store, BlessingRoots roots) {
		this.nativePtr = nativePtr;
		this.signer = signer;
		this.store = store;
		this.roots = roots;
	}

	@Override
	public Blessings bless(ECPublicKey key, Blessings with, String extension, Caveat caveat,
		Caveat... additionalCaveats) throws VeyronException {
		return nativeBless(this.nativePtr, key, with, extension, caveat, additionalCaveats);
	}
	@Override
	public Blessings blessSelf(String name, Caveat... caveats) throws VeyronException {
		return nativeBlessSelf(this.nativePtr, name, caveats);
	}
	@Override
	public Signature sign(byte[] message) throws VeyronException {
		if (this.signer != null) {
			final byte[] purpose = SecurityConstants.SIGNATURE_FOR_MESSAGE_SIGNING.getBytes();
			return this.signer.sign(purpose, message);
		}
		return nativeSign(this.nativePtr, message);
	}
	@Override
	public ECPublicKey publicKey() {
		if (this.signer != null) {
			return this.signer.publicKey();
		}
		try {
			return nativePublicKey(this.nativePtr);
		} catch (VeyronException e) {
			android.util.Log.e(TAG, "Couldn't get public key: " + e.getMessage());
			return null;
		}
	}
	@Override
	public Blessings[] blessingsByName(BlessingPattern name) {
		try {
			return nativeBlessingsByName(this.nativePtr, name);
		} catch (VeyronException e) {
			android.util.Log.e(TAG, "Couldn't get blessings for name: " + e.getMessage());
			return new Blessings[0];
		}
	}
	@Override
	public String[] blessingsInfo(Blessings blessings) {
		try {
			return nativeBlessingsInfo(this.nativePtr, blessings);
		} catch (VeyronException e) {
			android.util.Log.e(TAG,
				"Couldn't get human-readable strings for blessings: " + e.getMessage());
			return new String[0];
		}
	}
	@Override
	public BlessingStore blessingStore() {
		if (this.store != null) {
			return this.store;
		}
		try {
			return nativeBlessingStore(this.nativePtr);
		} catch (VeyronException e) {
			android.util.Log.e(TAG, "Couldn't get Blessing Store: " + e.getMessage());
			return null;
		}
	}
	@Override
	public BlessingRoots roots() {
		if (this.roots != null) {
			return this.roots;
		}
		try {
			return nativeRoots(this.nativePtr);
		} catch (VeyronException e) {
			android.util.Log.e(TAG, "Couldn't get Blessing Store: " + e.getMessage());
			return null;
		}
	}
	@Override
	public void addToRoots(Blessings blessings) throws VeyronException {
		nativeAddToRoots(this.nativePtr, blessings);
	}
	@Override
	public void finalize() {
		nativeFinalize(this.nativePtr);
	}
}