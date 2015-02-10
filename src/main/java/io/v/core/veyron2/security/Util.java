package io.v.core.veyron2.security;

import io.v.core.veyron2.verror2.VException;
import io.v.core.veyron2.util.VomUtil;

import java.security.interfaces.ECPublicKey;

/**
 * Util provides utilities for encoding/decoding various Veyron formats.  The encoding format
 * used here must match the encoding format used by the corresponding JNI Go library.
 */
class Util {
	/**
	 * VOM-encodes the provided WireBlessings.
	 *
	 * @param  wire WireBlessings to be encoded.
	 * @return      VOM-encoded WireBlessings.
	 */
	static byte[] encodeWireBlessings(WireBlessings wire) throws VException {
		return VomUtil.encode(wire, WireBlessings.class);
	}

	/**
	 * VOM-decodes the provided VOM-encoded WireBlessings.
	 *
	 * @param  encoded         VOM-encoded WireBlessings.
	 * @return                 decoded WireBlessings.
	 * @throws VException      if the provided WireBlessings couldn't be decoded.
	 */
	static WireBlessings decodeWireBlessings(byte[] encoded) throws VException {
		return (WireBlessings) VomUtil.decode(encoded, WireBlessings.class);
	}

	/**
	 * Encodes the provided BlessingPattern as a string.
	 *
	 * @param  pattern BlessingPattern to be encoded.
	 * @return         the encoded BlessingPattern.
	 */
	static String encodeBlessingPattern(BlessingPattern pattern) {
		if (pattern == null) {
			return "";
		}
		return pattern.getValue();
	}

	/**
	 * Decodes the encoded BlessingPattern encoded as a string.
	 *
	 * @param  encoded         BlessingPattern encoded as a string.
	 * @return                 decoded BlessingPattern.
	 */
	static BlessingPattern decodeBlessingPattern(String encoded) {
		if (encoded == null || encoded.isEmpty()) {
			return null;
		}
		return new BlessingPattern(encoded);
	}

	/**
	 * VOM-encodes the provided Signature.
	 *
	 * @param  signature Signature to be encoded.
	 * @return           the encoded Signature.
	 */
	static byte[] encodeSignature(Signature signature) throws VException {
		return VomUtil.encode(signature, Signature.class);
	}

	/**
	 * VOM-decodes the VOM-encoded Signature.
	 *
	 * @param  encoded         VOM-encoded Signature.
	 * @return                 decoded Signature.
	 * @throws VException      if the provided Signature couldn't be decoded.
	 */
	static Signature decodeSignature(byte[] encoded) throws VException {
		if (encoded == null || encoded.length == 0) {
			return null;
		}
		return (Signature) VomUtil.decode(encoded, Signature.class);
	}

	/**
	 * DER-encodes the provided ECPublicKey.
	 *
	 * @param  key ECPublicKey to be encoded.
	 * @return     the encoded ECPublicKey.
	 */
	static byte[] encodePublicKey(ECPublicKey key) {
		if (key == null) {
			return new byte[0];
		}
		return key.getEncoded();
	}

	/**
	 * Decodes the DER-encoded ECPublicKey.
	 *
	 * @param  encoded         DER-encoded ECPublicKey.
	 * @return                 decoded ECPublicKey.
	 * @throws VException      if the provided ECPublicKey couldn't be decoded.
	 */
	static ECPublicKey decodePublicKey(byte[] encoded) throws VException {
		if (encoded == null || encoded.length == 0) {
			return null;
		}
		return CryptoUtil.decodeECPublicKey(encoded);
	}

	/**
	 * VOM-encodes the provided Caveat.
	 *
	 * @param  caveat Caveat to be encoded.
	 * @return        the encoded Caveat.
	 */
	static byte[] encodeCaveat(Caveat caveat) throws VException {
		return VomUtil.encode(caveat, Caveat.class);
	}

	/**
	 * VOM-decodes the VOM-encoded Caveat.
	 *
	 * @param  encoded         VOM-encoded Caveat.
	 * @return                 decoded Caveat.
	 * @throws VException      if the provided Caveat couldn't be decoded.
	 */
	static Caveat decodeCaveat(byte[] encoded) throws VException {
		if (encoded == null || encoded.length == 0) {
			return null;
		}
		return (Caveat) VomUtil.decode(encoded, Caveat.class);
	}
}