package io.v.core.veyron2.security;

import io.v.core.veyron2.verror2.VException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.EllipticCurve;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

/**
 * CryptoUtil implements various cryptographic utilities.
 */
public class CryptoUtil {
	private static final String PK_ALGORITHM = "EC";

	// NIST-192
	@SuppressWarnings("unused")
    private static final ECParameterSpec EC_P192_PARAMS = getParameterSpec("prime192v1");
	@SuppressWarnings("unused")
    private static final ECParameterSpec EC_P224_PARAMS = getParameterSpec("secp224r1"); // NIST-224
	// NIST-256
	@SuppressWarnings("unused")
    private static final ECParameterSpec EC_P256_PARAMS = getParameterSpec("prime256v1");
	@SuppressWarnings("unused")
    private static final ECParameterSpec EC_P384_PARAMS = getParameterSpec("secp384r1"); // NIST-384
	@SuppressWarnings("unused")
    private static final ECParameterSpec EC_P521_PARAMS = getParameterSpec("secp521r1"); // NIST-521

	private static ECParameterSpec getParameterSpec(String algorithm) {
		try {
			final KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
			final ECGenParameterSpec spec = new ECGenParameterSpec(algorithm);  // NIST P-256
			gen.initialize(spec);
			return ((ECPublicKey)gen.generateKeyPair().getPublic()).getParams();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("EC crypto not supported!");
		} catch (InvalidAlgorithmParameterException e) {
			throw new RuntimeException("EC algorithm " + algorithm + " not supported!");
		}
	}

	/**
	 * Decodes the provided DER-encoded ECDSA public key.
	 *
	 * @param  encodedKey      DER-encoded ECDSA public key.
	 * @return                 ECDSA public key.
	 * @throws VException      if the public key could not be decoded.
	 */
	public static ECPublicKey decodeECPublicKey(byte[] encodedKey) throws VException {
		try {
			final X509EncodedKeySpec spec = new X509EncodedKeySpec(encodedKey);
			final KeyFactory factory = KeyFactory.getInstance(PK_ALGORITHM);
			return (ECPublicKey)factory.generatePublic(spec);
		} catch (NoSuchAlgorithmException e) {
			throw new VException(
				"Java runtime doesn't support " + PK_ALGORITHM + " algorithm: " + e.getMessage());
		} catch (InvalidKeySpecException e) {
			throw new VException("Encoded key is incompatible with " + PK_ALGORITHM +
				" algorithm: " + e.getMessage());
		}
	}

	/**
	 * Encodes the EC point into the uncompressed ANSI X9.62 format.
	 *
	 * @param curve            EC curve
	 * @param point            EC point
	 * @return                 ANSI X9.62-encoded EC point.
	 * @throws VException      if the curve and the point are incompatible.
	 */
	public static byte[] encodeECPoint(EllipticCurve curve, ECPoint point) throws VException {
		final int byteLen = (curve.getField().getFieldSize() + 7)  >> 3;
		final byte[] x = point.getAffineX().toByteArray();
		final byte[] y = point.getAffineY().toByteArray();
		if (x.length != byteLen) {
			throw new VException(String.format(
					"Illegal length for X axis of EC point, want %d have %d", byteLen, x.length));
		}
		if (y.length != byteLen) {
			throw new VException(String.format(
					"Illegal length for Y axis of EC point, want %d have %d", byteLen, y.length));
		}
		final byte[] xy = new byte[1 + 2 * byteLen];
		xy[0] = 4;
		System.arraycopy(x, 0, xy, 1, byteLen);
		System.arraycopy(y, 0, xy, 1 + byteLen, byteLen);
		return xy;
	}

	/**
	 * Decodes ANSI X9.62-encoded (uncompressed) EC point.
	 *
	 * @param  xy              ANSI X9.62-encoded EC point.
	 * @return                 EC point.
	 * @throws VException      if the EC point couldn't be decoded.
	 */
	public static ECPoint decodeECPoint(EllipticCurve curve, byte[] xy) throws VException {
		final int byteLen = (curve.getField().getFieldSize() + 7)  >> 3;
		if (xy.length != (1 + 2 * byteLen)) {
			throw new VException(String.format(
					"Data length mismatch: want %d have %d", (1 + 2 * byteLen), xy.length));
		}
		if (xy[0] != 4) { // compressed form
			throw new VException("Compressed curve formats not supported");
		}
		final BigInteger x = new BigInteger(Arrays.copyOfRange(xy, 1, 1 + byteLen));
		final BigInteger y = new BigInteger(Arrays.copyOfRange(xy, 1 + byteLen, xy.length));
		return new ECPoint(x, y);
	}

	/**
	 * Applies the specified cryptographic hash function on the provided message.
	 *
	 * @param  hashAlgorithm   name of the hash algorithm to use.
	 * @param  message         message to apply the hash function on.
	 * @return                 hashed message.
	 * @throws VException      if the message couldn't be hashed.
	 */
	public static byte[] hash(String hashAlgorithm, byte[] message) throws VException {
		try {
			final MessageDigest md = MessageDigest.getInstance(hashAlgorithm);
			md.update(message);
			final byte[] ret = md.digest();
			if (ret == null || ret.length == 0) {
				throw new VException("Got empty message after a hash using " + hashAlgorithm);
			}
			return ret;
		} catch (NoSuchAlgorithmException e) {
			throw new VException("Hashing algorithm " + hashAlgorithm + " not " +
				"supported by the runtime: " + e.getMessage());
		}
	}

	/**
	 * Creates a digest for the following message and the specified purpose, using the provided
	 * hash algorithm.
	 *
	 * @param  hashAlgorithm   name of the hash algorithm to use.
	 * @param  message         message that is part of the digest.
	 * @param  purpose         purpose that is part of the digest.
	 * @return                 digest for the specified message and digest.
	 * @throws VException      if there was an error creating a digest.
	 */
	static byte[] messageDigest(String hashAlgorithm,
		byte[] message, byte[] purpose) throws VException {
		if (message == null) {
			throw new VException("Empty message.");
		}
		if (purpose == null) {
			throw new VException("Empty purpose.");
		}
		message = hash(hashAlgorithm, message);
		purpose = hash(hashAlgorithm, purpose);
		final byte[] ret = join(message, purpose);
		return ret;
	}

	private static byte[] join(byte[] a, byte[] b) {
		if (a == null || a.length == 0) return b;
		if (b == null || b.length == 0) return a;
		final byte[] c = new byte[a.length + b.length];
		System.arraycopy(a, 0, c, 0, a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}

	/**
	 * Converts the provided Veyron signature into the ASN.1 format (used by Java).
	 *
	 * @param  sig             signature in Veyron format.
	 * @return                 signature in ASN.1 format.
	 * @throws VException      if the signature couldn't be converted.
	 */
	public static byte[] javaSignature(Signature sig) throws VException {
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
		final byte[] r = sig.getR();
		final byte[] s = sig.getS();
		if (r == null || r.length == 0) {
			throw new VException("Empty R component of signature.");
		}
		if (s == null || s.length == 0) {
			throw new VException("Empty S component of signature.");
		}
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(0x30);
		out.write(4 + r.length + s.length);
		out.write(0x02);
		out.write(r.length);
		out.write(r, 0, r.length);
		out.write(0x02);
		out.write(s.length);
		out.write(s, 0, s.length);
		return out.toByteArray();
	}

	/**
	 * Converts the provided Java signature (ASN.1 format) into the Veyron format.
	 *
	 * @param  hashAlgorithm   hash algorithm used when generating the signature.
	 * @param  purpose         purpose of the generated signature.
	 * @param  sig             signature in ASN.1 format.
	 * @return                 signature in Veyron format.
	 * @throws VException      if the signature couldn't be converted.
	 */
	public static Signature veyronSignature(String hashAlgorithm, byte[] purpose, byte[] sig)
		throws VException {
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
		final ByteArrayInputStream in = new ByteArrayInputStream(sig);
		int b;
		if ((b = in.read()) != 0x30) {
			throw new VException(String.format("Invalid signature type, want SEQUENCE (0x30), got 0x%02X", b));
		}
		if ((b = in.read()) != in.available()) {
			throw new VException(String.format("Invalid signature length, want %d, got %d", in.available(), b));
		}
		if ((b = in.read()) != 0x02) {
			throw new VException(String.format("Invalid type for R, want INTEGER (0x02), got 0x%02X", b));
		}
		if ((b = in.read()) > in.available()) {
			throw new VException(String.format("Invalid length for R, want less than %d, got %d", in.available(), b));
		}
		r = new byte[b];
		if (in.read(r, 0, b) != b) {
			throw new VException(String.format("Error reading %d bytes of R from signature", b));
		}
		if ((b = in.read()) != 0x02) {
			throw new VException(String.format("Invalid type for S, want INTEGER (0x02), got 0x%02X", b));
		}
		if ((b = in.read()) > in.available()) {
			throw new VException(String.format("Invalid length for S, want less than %d, got %d", in.available(), b));
		}
		s = new byte[b];
		if (in.read(s, 0, b) != b) {
			throw new VException(String.format("Error reading %d bytes of S from signature", b));
		}
		return new Signature(purpose, new Hash(hashAlgorithm), r, s);
	}
}