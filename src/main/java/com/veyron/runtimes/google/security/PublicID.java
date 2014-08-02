package com.veyron.runtimes.google.security;

import com.veyron2.ipc.VeyronException;
import com.veyron2.security.PrincipalPattern;
import com.veyron2.security.ServiceCaveat;

import java.security.interfaces.ECPublicKey;

public class PublicID implements com.veyron2.security.PublicID {
	private final long nativePtr;

	private native String[] nativeNames(long nativePtr);
	private native boolean nativeMatch(long nativePtr, String pattern);
	private native byte[] nativePublicKey(long nativePtr) throws VeyronException;
	private native long nativeAuthorize(long nativePtr, com.veyron2.security.Context context)
		throws VeyronException;
	private native ServiceCaveat[] nativeThirdPartyCaveats(long nativePtr);
	private native boolean nativeEquals(long nativePtr, long otherNativePtr);
	private native void nativeFinalize(long nativePtr);

	public PublicID(long nativePtr) {
		this.nativePtr = nativePtr;
	}
	// Implements com.veyron2.security.PublicID.
	@Override
	public String[] names() {
		return nativeNames(this.nativePtr);
	}
	@Override
	public boolean match(PrincipalPattern pattern) {
		return nativeMatch(this.nativePtr, pattern.getValue());
	}
	@Override
	public ECPublicKey publicKey() {
		try {
			final byte[] encodedKey = this.nativePublicKey(this.nativePtr);
			return CryptoUtil.decodeECPublicKey(encodedKey);
		} catch (VeyronException e) {
			throw new RuntimeException(
				"Couldn't decode native ECDSA public key: " + e.getMessage());
		}
	}
	@Override
	public com.veyron2.security.PublicID authorize(com.veyron2.security.Context context)
		throws VeyronException {
		return new PublicID(nativeAuthorize(this.nativePtr, context));
	}
	@Override
	public ServiceCaveat[] thirdPartyCaveats() {
		return nativeThirdPartyCaveats(this.nativePtr);
	}
	// Implements java.lang.Object.
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof PublicID)) return false;
		final PublicID other = (PublicID)obj;
		return nativeEquals(this.nativePtr, other.nativePtr);
	}
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
	/**
	 * Returns the pointer to the native implementation.
	 *
	 * @return the pointer to the native implementation.
	 */
	private long getNativePtr() { return this.nativePtr; }
}