package io.veyron.veyron.veyron2.security;

import android.security.KeyPairGeneratorSpec;

import io.veyron.veyron.veyron2.ipc.VeyronException;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Calendar;

import javax.security.auth.x500.X500Principal;

/**
 * CryptoUtil implements various cryptographic utilities.
 */
public class CryptoUtil {
	private static final String KEYSTORE = "AndroidKeyStore";
	private static final String PK_ALGORITHM = "EC";
	private static final int KEY_SIZE = 256;

	// NIST-192
	private static final ECParameterSpec EC_P192_PARAMS = getParameterSpec("prime192v1");
	private static final ECParameterSpec EC_P224_PARAMS = getParameterSpec("secp224r1"); // NIST-224
	// NIST-256
	private static final ECParameterSpec EC_P256_PARAMS = getParameterSpec("prime256v1");
	private static final ECParameterSpec EC_P384_PARAMS = getParameterSpec("secp384r1"); // NIST-384
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
	 * Generates a new private key and stores it in the Android KeyStore under the provided alias.
	 * If a key already exists under the provided alias, it will be overwritten.
	 * Throws an exception if the key could not be generated.
	 *
	 * @param  ctx             android Context.
	 * @param  alias           alias under which the private key will be stored inside the KeyStore.
	 * @return                 an entry storing the private key and the certificate chain for the
	 *                         corresponding public key.
	 * @throws VeyronException if the key could not be generated.
	 */
	public static KeyStore.PrivateKeyEntry genKeyStorePrivateKey(
		android.content.Context ctx, String alias) throws VeyronException {
		try {
			// NOTE(spetrovic): KeyPairGenerator needs to be initialized with "RSA" algorithm and
			// not "EC" algorithm, even though we generate "EC" keys below.  Otherwise, Android
			// KeyStore claims that "EC" is an unrecognized algorithm!
			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", KEYSTORE);
			final Calendar notBefore = Calendar.getInstance();
			final Calendar notAfter = Calendar.getInstance();
			notAfter.add(1, Calendar.YEAR);
			final KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(ctx)
						.setAlias(alias)
						.setKeyType(PK_ALGORITHM)
						.setKeySize(KEY_SIZE)
						.setSubject(new X500Principal(
							String.format("CN=%s, OU=%s", alias, ctx.getPackageName())))
						.setSerialNumber(BigInteger.ONE)
						.setStartDate(notBefore.getTime())
						.setEndDate(notAfter.getTime())
						.build();
			keyGen.initialize(spec);
			keyGen.generateKeyPair();
			return getKeyStorePrivateKey(alias);
		} catch (NoSuchProviderException e) {
			throw new VeyronException("Couldn't find Android KeyStore");
		} catch (NoSuchAlgorithmException e) {
			throw new VeyronException(
					"Keystore doesn't support " + PK_ALGORITHM + " algorithm: " + e.getMessage());
		} catch (InvalidAlgorithmParameterException e) {
			throw new VeyronException("Invalid keystore algorithm parameters: " + e.getMessage());
		}
	}

	/**
	 * Returns the private key if it exists in the Android KeyStore or null if it doesn't exist.
	 * Throws an exception on an error.
	 *
	 * @param  alias           alias of the key in the KeyStore.
	 * @return                 an entry storing the private key and the certificate chain for the
	 *                         corresponding public key.
	 * @throws VeyronException if the key could not be retrieved.
	 */
	public static KeyStore.PrivateKeyEntry getKeyStorePrivateKey(String alias)
			throws VeyronException {
		try {
			final KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
			keyStore.load(null);
			final KeyStore.Entry entry = keyStore.getEntry(alias, null);
			if (entry == null) {
				return null;
			}
			if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
				throw new VeyronException(
						"Entry " + alias + " exists but not a private key entry.");
			}
			return (KeyStore.PrivateKeyEntry)entry;
		} catch (KeyStoreException e) {
			throw new VeyronException("KeyStore not initialized: " + e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			throw new VeyronException("KeyStore doesn't support the algorithm: " + e.getMessage());
		} catch (IOException e) {
			throw new VeyronException("Error loading keystore: " + e.getMessage());
		} catch (CertificateException e) {
			throw new VeyronException("Error loading keystore certificates: " + e.getMessage());
		} catch (UnrecoverableEntryException e) {
			throw new VeyronException("Couldn't get keystore entry: " + e.getMessage());
		}
	}

	/**
	 * Decodes the provided DER-encoded ECDSA public key.
	 *
	 * @param  encodedKey      DER-encoded ECDSA public key.
	 * @return                 ECDSA public key.
	 * @throws VeyronException if the public key could not be decoded.
	 */
	public static ECPublicKey decodeECPublicKey(byte[] encodedKey) throws VeyronException {
		try {
			final X509EncodedKeySpec spec = new X509EncodedKeySpec(encodedKey);
			final KeyFactory factory = KeyFactory.getInstance(PK_ALGORITHM);
			return (ECPublicKey)factory.generatePublic(spec);
		} catch (NoSuchAlgorithmException e) {
			throw new VeyronException(
				"Java runtime doesn't support " + PK_ALGORITHM + " algorithm: " + e.getMessage());
		} catch (InvalidKeySpecException e) {
			throw new VeyronException("Encoded key is incompatible with " + PK_ALGORITHM +
				" algorithm: " + e.getMessage());
		}
	}

	/**
	 * Encodes the EC point into the uncompressed ANSI X9.62 format.
	 *
	 * @param curve            EC curve
	 * @param point            EC point
	 * @return                 ANSI X9.62-encoded EC point.
	 * @throws VeyronException if the curve and the point are incompatible.
	 */
	public static byte[] encodeECPoint(EllipticCurve curve, ECPoint point) throws VeyronException {
		final int byteLen = (curve.getField().getFieldSize() + 7)  >> 3;
		final byte[] x = point.getAffineX().toByteArray();
		final byte[] y = point.getAffineY().toByteArray();
		if (x.length != byteLen) {
			throw new VeyronException(String.format(
					"Illegal length for X axis of EC point, want %d have %d", byteLen, x.length));
		}
		if (y.length != byteLen) {
			throw new VeyronException(String.format(
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
	 * @throws VeyronException if the EC point couldn't be decoded.
	 */
	public static ECPoint decodeECPoint(EllipticCurve curve, byte[] xy) throws VeyronException {
		final int byteLen = (curve.getField().getFieldSize() + 7)  >> 3;
		if (xy.length != (1 + 2 * byteLen)) {
			throw new VeyronException(String.format(
					"Data length mismatch: want %d have %d", (1 + 2 * byteLen), xy.length));
		}
		if (xy[0] != 4) { // compressed form
			throw new VeyronException("Compressed curve formats not supported");
		}
		final BigInteger x = new BigInteger(Arrays.copyOfRange(xy, 1, 1 + byteLen));
		final BigInteger y = new BigInteger(Arrays.copyOfRange(xy, 1 + byteLen, xy.length));
		return new ECPoint(x, y);
	}
}