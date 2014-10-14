package io.veyron.veyron.veyron.runtimes.google.security;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.security.wire.ChainPublicID;
import io.veyron.veyron.veyron2.vdl.JSONUtil;

/**
 * Util provides utilities for encoding/decoding PublicID chains.
 */
public class Util {
	/**
	 * JSON-encodes the provides set of chains.
	 *
	 * @param chains           set of chains to be encoded.
	 * @return                 JSON-encoded chains.
	 * @throws VeyronException if the provided set of chains couldn't be encoded.
	 */
	public static String[] encodeChains(ChainPublicID[] chains) throws VeyronException {
		if (chains == null) {
			throw new VeyronException("Empty chains");
		}
		final Gson gson = JSONUtil.getGsonBuilder().create();
		final String[] ret = new String[chains.length];
		for (int i = 0; i < chains.length; ++i) {
			ret[i] = gson.toJson(chains[i]);
		}
		return ret;
	}

	/**
	 * JSON-decodes the provides set of encoded chains.
	 *
	 * @param chains           set of JSON-encoded chains to be decoded.
	 * @return                 JSON-decoded chains.
	 * @throws VeyronException if the provided set of chains couldn't be decoded.
	 */
	public static ChainPublicID[] decodeChains(String[] chains) throws VeyronException {
		if (chains == null) {
			throw new VeyronException("Empty chains");
		}
		final Gson gson = JSONUtil.getGsonBuilder().create();
		final ChainPublicID[] ret = new ChainPublicID[chains.length];
		for (int i = 0; i < chains.length; ++i) {
			try {
				ret[i] = gson.fromJson(chains[i], new TypeToken<ChainPublicID>(){}.getType());
			} catch (JsonSyntaxException e) {
				throw new VeyronException("Invalid chain string: " + e.getMessage());
			}
		}
		return ret;
	}
}