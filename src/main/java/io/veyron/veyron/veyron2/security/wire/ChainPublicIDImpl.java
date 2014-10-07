package io.veyron.veyron.veyron2.security.wire;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.security.Context;
import io.veyron.veyron.veyron2.security.CryptoUtil;
import io.veyron.veyron.veyron2.security.PublicID;
import io.veyron.veyron.veyron2.vdl.JSONUtil;

import java.security.interfaces.ECPublicKey;
import java.util.List;

/**
 * ChainPublicIDImpl is an implementation of PublicID that uses ChainPublicID wire format as its
 * underlying data.
 */
public class ChainPublicIDImpl implements PublicID {
	private final ChainPublicID id;
	private final String name;
	private final ECPublicKey key;

	/**
	 * Creates a new PublicID using the provided ChainPublicID as its underlying data.
	 *
	 * @param  id              wire data.
	 * @returns                new PublicID.
	 * @throws VeyronException if the PublicID couldn't be created.
	 */
	public ChainPublicIDImpl(ChainPublicID id) throws VeyronException {
		if (id.getCertificates() == null || id.getCertificates().size() == 0) {
			throw new VeyronException("Empty certificate chain.");
		}
		this.id = id;
		this.name = getName(id);
		this.key = getKey(id);
	}

	/**
	 * Creates a new PublicID using the JSON-encoded ChainPublicID as its underlying data.
	 *
	 * @param  jsonEncodedID   JSON-encoded ChainPublicID.
	 * @returns                new PublicID.
	 * @throws VeyronException if the PublicID couldn't be created.
	 */
	public ChainPublicIDImpl(String jsonEncodedID) throws VeyronException {
		this(decodeChainID(jsonEncodedID));
	}

	@Override
	public String[] names() {
		return new String[]{ this.name };
	}

	@Override
	public ECPublicKey publicKey() {
		return this.key;
	}

	@Override
	public PublicID authorize(Context context) throws VeyronException {
		// TODO(spetrovic): really need to verify caveats here.
		return this;
	}
	@Override
	public ChainPublicID[] encode() throws VeyronException {
		return new ChainPublicID[]{ this.id };
	}

	private static String getName(ChainPublicID id) throws VeyronException {
		String name = "";
		for (Certificate c : id.getCertificates()) {
			if (c.getName().isEmpty()) {
				throw new VeyronException("Empty certificate name");
			}
			if (!name.isEmpty()) {
				name += "/";
			}
			name += c.getName();
		}
		return name;
	}

	private static ECPublicKey getKey(ChainPublicID id) throws VeyronException {
		final List<Certificate> certs = id.getCertificates();
		final PublicKey key = certs.get(certs.size() - 1).getPublicKey();
		return CryptoUtil.decodeECPublicKey(key);
	}

	private static ChainPublicID decodeChainID(String id) throws VeyronException {
		try {
			final Gson gson = JSONUtil.getGsonBuilder().create();
			return gson.fromJson(id, new TypeToken<ChainPublicID>(){}.getType());
		} catch (JsonSyntaxException e) {
			throw new VeyronException("Invalid chain string: " + e.getMessage());
		}
	}
}