package io.veyron.veyron.veyron2.security;

import com.google.common.reflect.TypeToken;

import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.VomUtil;

import java.math.BigInteger;
import java.security.interfaces.ECPublicKey;

/**
 * Util provides utilities for encoding/decoding various Veyron formats.  The encoding format
 * used here must match the encoding format used by the corresponding JNI Go library.
 */
class Util {
	/**
	 * Encodes the provided Blessings as VOM-encoded WireBlessings.
	 *
	 * @param  blessings       Blessings to be encoded.
	 * @return                 VOM-encoded WireBlessings.
	 */
	static byte[] encodeBlessings(Blessings blessings) throws VeyronException {
		if (blessings == null) {
			return new byte[0];
		}
		final WireBlessings wire = blessings.wireFormat();
		return encodeWireBlessings(wire);
	}

	/**
	 * Decodes the provided Blessings encoded as VOM-encoded WireBlessings.
	 *
	 * @param  encoded         Blessings encoded as VOM-encoded WireBlessings.
	 * @return                 decoded Blessings.
	 * @throws VeyronException if the provided Blessings couldn't be decoded.
	 */
	static Blessings decodeBlessings(byte[] encoded) throws VeyronException {
		if (encoded == null || encoded.length == 0) {
			return null;
		}
		final WireBlessings wire = decodeWireBlessings(encoded);
		return BlessingsImpl.create(wire);
	}

	/**
	 * VOM-encodes the provided WireBlessings.
	 *
	 * @param  wire WireBlessings to be encoded.
	 * @return      VOM-encoded WireBlessings.
	 */
	static byte[] encodeWireBlessings(WireBlessings wire) throws VeyronException {
		return VomUtil.encode(wire, new TypeToken<WireBlessings>(){}.getType());
	}

	/**
	 * VOM-decodes the provided VOM-encoded WireBlessings.
	 *
	 * @param  encoded         VOM-encoded WireBlessings.
	 * @return                 decoded WireBlessings.
	 * @throws VeyronException if the provided WireBlessings couldn't be decoded.
	 */
	static WireBlessings decodeWireBlessings(byte[] encoded) throws VeyronException {
		return (WireBlessings) VomUtil.decode(encoded, new TypeToken<WireBlessings>(){}.getType());
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
	static byte[] encodeSignature(Signature signature) throws VeyronException {
		return VomUtil.encode(signature, new TypeToken<Signature>(){}.getType());
	}

	/**
	 * VOM-decodes the VOM-encoded Signature.
	 *
	 * @param  encoded         VOM-encoded Signature.
	 * @return                 decoded Signature.
	 * @throws VeyronException if the provided Signature couldn't be decoded.
	 */
	static Signature decodeSignature(byte[] encoded) throws VeyronException {
		if (encoded == null || encoded.length == 0) {
			return null;
		}
		return (Signature) VomUtil.decode(encoded, new TypeToken<Signature>(){}.getType());
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
	 * @throws VeyronException if the provided ECPublicKey couldn't be decoded.
	 */
	static ECPublicKey decodePublicKey(byte[] encoded) throws VeyronException {
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
	static byte[] encodeCaveat(Caveat caveat) throws VeyronException {
		return VomUtil.encode(caveat, new TypeToken<Caveat>(){}.getType());
	}

	/**
	 * VOM-decodes the VOM-encoded Caveat.
	 *
	 * @param  encoded         VOM-encoded Caveat.
	 * @return                 decoded Caveat.
	 * @throws VeyronException if the provided Caveat couldn't be decoded.
	 */
	static Caveat decodeCaveat(byte[] encoded) throws VeyronException {
		if (encoded == null || encoded.length == 0) {
			return null;
		}
		return (Caveat) VomUtil.decode(encoded, new TypeToken<Caveat>(){}.getType());
	}

	/**
	 * VOM-encodes the provided Caveat array.
	 *
	 * @param  caveats Caveat array to be encoded.
	 * @return         the encoded Caveat array.
	 */
	static byte[] encodeCaveats(Caveat[] caveats) throws VeyronException {
		return VomUtil.encode(caveats, new TypeToken<Caveat[]>(){}.getType());
	}

	/**
	 * VOM-decodes the VOM-encoded Caveat array.
	 *
	 * @param  encoded         VOM-encoded Caveat array.
	 * @return                 decoded Caveat array.
	 * @throws VeyronException if the provided Caveat array couldn't be decoded.
	 */
	static Caveat[] decodeCaveats(byte[] encoded) throws VeyronException {
		if (encoded == null || encoded.length == 0) {
			return null;
		}
		return (Caveat[]) VomUtil.decode(encoded, new TypeToken<Caveat[]>(){}.getType());
	}
}