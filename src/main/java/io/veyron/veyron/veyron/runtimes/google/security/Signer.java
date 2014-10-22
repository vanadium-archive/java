package io.veyron.veyron.veyron.runtimes.google.security;

import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.security.Hash;
import io.veyron.veyron.veyron2.security.Signature;

import java.io.ByteArrayInputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;

public class Signer implements io.veyron.veyron.veyron2.security.Signer {
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

	private static byte[] hash(byte[] message) throws VeyronException {
		try {
			final MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
			md.update(message);
			final byte[] ret = md.digest();
			if (ret == null || ret.length == 0) {
				throw new VeyronException("Got empty message after a hash using " + HASH_ALGORITHM);
			}
			return ret;
		} catch (NoSuchAlgorithmException e) {
			throw new VeyronException("Hashing algorithm " + HASH_ALGORITHM + " not " +
				"supported by the runtime: " + e.getMessage());
		}
	}

	private final PrivateKey privKey;
	private final ECPublicKey pubKey;

	public Signer(PrivateKey privKey, ECPublicKey pubKey) {
		this.privKey = privKey;
		this.pubKey = pubKey;
	}

	@Override
	public Signature sign(byte[] purpose, byte[] message) throws VeyronException {
		if (message == null) {
			throw new VeyronException("Cannot sign empty message.");
		}
		message = hash(message);
		if (purpose != null) {
			final byte[] purposeHash = hash(purpose);
			message = join(message, purposeHash);
		}
		// Sign.  Note that the signer will first apply another hash on the message, resulting in:
		// ECDSA.Sign(Hash(Hash(message) + Hash(purpose))).
		try {
			final java.security.Signature sig = java.security.Signature.getInstance(SIGN_ALGORITHM);
			sig.initSign(this.privKey);
			sig.update(message);
			final byte[] encoded = sig.sign();
			return decodeSignature(purpose, encoded);
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

	private Signature decodeSignature(byte[] purpose, byte[] encodedMsg) throws VeyronException {
		byte[] r, s;
		// The ASN.1 format of the signature should be:
		//    Signature ::= SEQUENCE {
		//       r   INTEGER,
		//       s   INTEGER
		//    }
		// When encoded, this translates into the following byte sequence:
		//    0x30 len 0x02 rlen [r bytes] 0x02 slen [s bytes].
		//
		// Note that we could have used BouncyCastle or an ASN1-decoding package to decode
		// the byte sequence, but the encoding is simple enough that we can do it by hand.
		final ByteArrayInputStream in = new ByteArrayInputStream(encodedMsg);
		int b;
		if ((b = in.read()) != 0x30) {
			throw new VeyronException(String.format("Invalid signature type, want SEQUENCE (0x30), got 0x%02X", b));
		}
		if ((b = in.read()) != in.available()) {
			throw new VeyronException(String.format("Invalid signature length, want %d, got %d", in.available(), b));
		}
		if ((b = in.read()) != 0x02) {
			throw new VeyronException(String.format("Invalid type for R, want INTEGER (0x02), got 0x%02X", b));
		}
		if ((b = in.read()) > in.available()) {
			throw new VeyronException(String.format("Invalid length for R, want less than %d, got %d", in.available(), b));
		}
		r = new byte[b];
		if (in.read(r, 0, b) != b) {
			throw new VeyronException(String.format("Error reading %d bytes of R from signature", b));
		}
		if ((b = in.read()) != 0x02) {
			throw new VeyronException(String.format("Invalid type for S, want INTEGER (0x02), got 0x%02X", b));
		}
		if ((b = in.read()) > in.available()) {
			throw new VeyronException(String.format("Invalid length for S, want less than %d, got %d", in.available(), b));
		}
		s = new byte[b];
		if (in.read(s, 0, b) != b) {
			throw new VeyronException(String.format("Error reading %d bytes of S from signature", b));
		}
		return new Signature(purpose, new Hash(HASH_ALGORITHM), r, s);
	}
}
