package io.veyron.veyron.veyron2.security;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonSyntaxException;

import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.vdl.JSONUtil;

import java.security.interfaces.ECPublicKey;

/**
 * Util provides utilities for encoding/decoding various Veyron formats.  The encoding format
 * used here must match the encoding format used by the corresponding JNI Go library.
 */
class Util {
	/**
	 * Encodes the provided Blessings as JSON-encoded WireBlessings.
	 *
	 * @param blessings        Blessings to be encoded.
	 * @return                 JSON-encoded WireBlessings.
	 */
	static String encodeBlessings(Blessings blessings) {
		if (blessings == null) {
			return "";
		}
		final WireBlessings wire = blessings.wireFormat();
		return encodeWireBlessings(wire);
	}

	/**
	 * Decodes the provided Blessings encoded as JSON-encoded WireBlessings.
	 *
	 * @param encoded          Blessings encoded as JSON-encoded WireBlessings.
	 * @return                 decoded Blessings.
	 * @throws VeyronException if the provided Blessings couldn't be decoded.
	 */
	static Blessings decodeBlessings(String encoded) throws VeyronException {
		if (encoded.isEmpty()) {
			return null;
		}
		final WireBlessings wire = decodeWireBlessings(encoded);
		return BlessingsImpl.create(wire);
	}

	/**
	 * JSON-encodes the provided WireBlessings.
	 *
	 * @param  wire WireBlessings to be encoded.
	 * @return      JSON-encoded WireBlessings.
	 */
	static String encodeWireBlessings(WireBlessings wire) {
		if (wire == null) {
			return "";
		}
		return JSONUtil.getGsonBuilder().create().toJson(wire);
	}

	/**
	 * Decodes the provided JSON-encoded WireBlessings.
	 *
	 * @param  encoded         JSON-encoded WireBlessings.
	 * @return                 decoded WireBlessings.
	 * @throws VeyronException if the provided WireBlessings couldn't be decoded.
	 */
	static WireBlessings decodeWireBlessings(String encoded) throws VeyronException {
		if (encoded.isEmpty()) {
			return null;
		}
		try {
			return JSONUtil.getGsonBuilder().create().fromJson(encoded,
				new TypeToken<WireBlessings>(){}.getType());
		} catch (JsonSyntaxException e) {
			throw new VeyronException(String.format("Invalid WireBlessings encoded string %s: %s",
				encoded, e.getMessage()));
		}
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
		if (encoded.isEmpty()) {
			return null;
		}
		return new BlessingPattern(encoded);
	}

	/**
	 * JSON-encodes the provided Signature.
	 *
	 * @param  signature Signature to be encoded.
	 * @return           the encoded Signature.
	 */
	static String encodeSignature(Signature signature) {
		if (signature == null) {
			return "";
		}
		return JSONUtil.getGsonBuilder().create().toJson(signature);
	}

	/**
	 * Decodes the JSON-encoded Signature.
	 *
	 * @param  encoded         JSON-encoded Signature.
	 * @return                 decoded Signature.
	 * @throws VeyronException if the provided Signature couldn't be decoded.
	 */
	static Signature decodeSignature(String encoded) throws VeyronException {
		if (encoded.isEmpty()) {
			return null;
		}
		try {
			return JSONUtil.getGsonBuilder().create().fromJson(encoded,
				new TypeToken<Signature>(){}.getType());
		} catch (JsonSyntaxException e) {
			throw new VeyronException(String.format("Invalid Signature encoded string %s: %s",
				encoded, e.getMessage()));
		}
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
		if (encoded.length == 0) {
			return null;
		}
		return CryptoUtil.decodeECPublicKey(encoded);
	}

	/**
	 * JSON-encodes the provided Caveat.
	 *
	 * @param  caveat Caveat to be encoded.
	 * @return        the encoded Caveat.
	 */
	static String encodeCaveat(Caveat caveat) {
		if (caveat == null) {
			return "";
		}
		return JSONUtil.getGsonBuilder().create().toJson(caveat);
	}

	/**
	 * Decodes the JSON-encoded Caveat.
	 *
	 * @param  encoded         JSON-encoded Caveat.
	 * @return                 decoded Caveat.
	 * @throws VeyronException if the provided Caveat couldn't be decoded.
	 */
	static Caveat decodeCaveat(String encoded) throws VeyronException {
		if (encoded.isEmpty()) {
			return null;
		}
		try {
			return JSONUtil.getGsonBuilder().create().fromJson(encoded,
				new TypeToken<Caveat>(){}.getType());
		} catch (JsonSyntaxException e) {
			throw new VeyronException(String.format("Invalid Caveat encoded string %s: %s",
				encoded, e.getMessage()));
		}
	}

	/**
	 * JSON-encodes the provided Caveat array.
	 *
	 * @param  caveats Caveat array to be encoded.
	 * @return         the encoded Caveat array.
	 */
	static String encodeCaveats(Caveat[] caveats) {
		if (caveats == null) {
			return "";
		}
		return JSONUtil.getGsonBuilder().create().toJson(caveats);
	}

	/**
	 * Decodes the JSON-encoded Caveat array.
	 *
	 * @param  encoded         JSON-encoded Caveat array.
	 * @return                 decoded Caveat array.
	 * @throws VeyronException if the provided Caveat array couldn't be decoded.
	 */
	static Caveat[] decodeCaveats(String encoded) throws VeyronException {
		if (encoded.isEmpty()) {
			return null;
		}
		try {
			return JSONUtil.getGsonBuilder().create().fromJson(encoded,
				new TypeToken<Caveat[]>(){}.getType());
		} catch (JsonSyntaxException e) {
			throw new VeyronException(String.format("Invalid Caveat array encoded string %s: %s",
				encoded, e.getMessage()));
		}
	}
}