package io.veyron.veyron.veyron2.services.security.access;

import com.google.common.reflect.TypeToken;
import com.google.gson.JsonSyntaxException;

import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.vdl.JSONUtil;

/**
 * Util provides utilities for encoding/decoding various Veyron formats.  The encoding format
 * used here must match the encoding format used by the corresponding JNI Go library.
 */
class Util {
	/**
	 * JSON-encodes the provided ACL.
	 *
	 * @param  acl  ACL to be encoded.
	 * @return      JSON-encoded ACL.
	 */
	static String encodeACL(ACL acl) {
		if (acl == null) {
			return "";
		}
		return JSONUtil.getGsonBuilder().create().toJson(acl);
	}

	/**
	 * Decodes the provided JSON-encoded ACL.
	 *
	 * @param  encoded         JSON-encoded ACL.
	 * @return                 decoded ACL.
	 * @throws VeyronException if the provided ACL couldn't be decoded.
	 */
	static ACL decodeACL(String encoded) throws VeyronException {
		if (encoded.isEmpty()) {
			return null;
		}
		try {
			return JSONUtil.getGsonBuilder().create().fromJson(encoded,
				new TypeToken<ACL>(){}.getType());
		} catch (JsonSyntaxException e) {
			throw new VeyronException(String.format("Invalid ACL encoded string %s: %s",
				encoded, e.getMessage()));
		}
	}
}