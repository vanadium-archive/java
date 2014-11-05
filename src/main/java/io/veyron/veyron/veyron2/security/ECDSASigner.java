package io.veyron.veyron.veyron2.security;

import io.veyron.veyron.veyron2.ipc.VeyronException;

import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

public class ECDSASigner implements io.veyron.veyron.veyron2.security.Signer {
	private static final String TAG = "Veyron runtime";
	private static final String HASH_ALGORITHM = "SHA256";
	private static final String SIGN_ALGORITHM = HASH_ALGORITHM + "withECDSA";

	private static byte[] join(byte[] a, byte[] b) {
		if (a == null || a.length == 0) return b;
		if (b == null || b.length == 0) return a;
		final byte[] c = new byte[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}


	private final PrivateKey privKey;
	private final ECPublicKey pubKey;

	public ECDSASigner(PrivateKey privKey, ECPublicKey pubKey) {
		this.privKey = privKey;
		this.pubKey = pubKey;
	}

	@Override
	public Signature sign(byte[] purpose, byte[] message) throws VeyronException {
		message = CryptoUtil.messageDigest(HASH_ALGORITHM, message, purpose);
		// Sign.  Note that the signer will first apply another hash on the message, resulting in:
		// ECDSA.Sign(Hash(Hash(message) + Hash(purpose))).
		try {
			final java.security.Signature sig = java.security.Signature.getInstance(SIGN_ALGORITHM);
			sig.initSign(this.privKey);
			sig.update(message);
			final byte[] asn1Sig = sig.sign();
			return CryptoUtil.veyronSignature(HASH_ALGORITHM, purpose, asn1Sig);
		} catch (NoSuchAlgorithmException e) {
			throw new VeyronException("Signing algorithm " + SIGN_ALGORITHM +
				" not supported by the runtime: " + e.getMessage());
		} catch (InvalidKeyException e) {
			throw new VeyronException("Invalid private key: " + e.getMessage());
		} catch (SignatureException e) {
			throw new VeyronException(
				"Invalid signing data [ " + Arrays.toString(message) + " ]: " + e.getMessage());
		}
	}

	@Override
	public ECPublicKey publicKey() {
		return this.pubKey;
	}
}
