package com.veyron.runtimes.google.security;

import com.veyron2.ipc.VeyronException;
import com.veyron2.security.ServiceCaveat;

import com.veyron2.security.PrincipalPattern;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;

public class PublicID implements com.veyron2.security.PublicID {
	// Curve parameters for the 224-, 256-, 384-, and 521-bit EC algorithms.
	private static final ECParameterSpec params224 = initSpec(224);
	private static final ECParameterSpec params256 = initSpec(256);
	private static final ECParameterSpec params384 = initSpec(384);
	private static final ECParameterSpec params521 = initSpec(521);

	private static ECParameterSpec initSpec(int bitSize) {
		try {
			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
			final SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
	    	keyGen.initialize(bitSize, random);
	    	final ECPublicKey pub = (ECPublicKey)keyGen.generateKeyPair().getPublic();
	    	return pub.getParams();
	    } catch (NoSuchAlgorithmException e) {
	    	throw new RuntimeException(
				String.format("Couldn't find EC%d crypto algorithm: %s", bitSize, e.getMessage()));
	    }
	}

	private static ECParameterSpec getSpec(int bitSize) {
		switch (bitSize) {
		case 224: return PublicID.params224;
		case 256: return PublicID.params256;
		case 384: return PublicID.params384;
		case 521: return PublicID.params521;
		}
		return null;
	}

	public static ECPublicKeyInfo getKeyInfo(ECPublicKey key) {
		return new ECPublicKeyInfo(
			key.getW().getAffineX().toByteArray(),
			key.getW().getAffineY().toByteArray(),
			key.getEncoded(),
			key.getParams().getCurve().getField().getFieldSize());
	}

	private final long nativePtr;

	private native String[] nativeNames(long nativePtr);
	private native boolean nativeMatch(long nativePtr, String pattern);
	private native ECPublicKeyInfo nativePublicKey(long nativePtr) throws VeyronException;
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
			final ECPublicKeyInfo info = this.nativePublicKey(this.nativePtr);
			final ECPoint key = new ECPoint(new BigInteger(info.keyX), new BigInteger(info.keyY));
			final ECParameterSpec spec = getSpec(info.curveFieldBitSize);
			if (spec == null) {  // unknown curve
				return null;
			}
			return new ECPublicKey() {
				@Override
				public ECPoint getW() { return key; }
				@Override
				public String getAlgorithm() { return "EC"; }
				@Override
				public String getFormat() { return "X.509"; }
				@Override
				public byte[] getEncoded() { return info.encodedKey; }
				@Override
				public ECParameterSpec getParams() { return spec; }
			};
		} catch (NumberFormatException e) {
			return null;
		} catch (VeyronException e) {
			return null;
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

	private static class ECPublicKeyInfo {
		final byte[] keyX, keyY, encodedKey;
		final int curveFieldBitSize;

		public ECPublicKeyInfo(byte[] keyX, byte[] keyY, byte[] encodedKey, int bitSize) {
			this.keyX = keyX;
			this.keyY = keyY;
			this.encodedKey = encodedKey;
			this.curveFieldBitSize = bitSize;
		}
	}
}