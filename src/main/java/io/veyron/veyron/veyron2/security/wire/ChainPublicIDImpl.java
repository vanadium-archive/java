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
 * ChainPublicIDImpl is an implementation of PublicID that uses credentials in the provided
 * ChainPublicID.
 */
public class ChainPublicIDImpl implements PublicID {
	private final ChainPublicID[] ids;
	private final String[] names;
	private final ECPublicKey key;

	/**
	 * Creates a new PublicID using the credentials in the provided ChainPublicID.
	 *
	 * @param  id              wire data.
	 * @return                 new PublicID.
	 * @throws VeyronException if the PublicID couldn't be created.
	 */
	public ChainPublicIDImpl(ChainPublicID id) throws VeyronException {
		this(new ChainPublicID[]{ id });
	}

	/**
	 * Creates a new PublicID using the combined credentials of the provided ChainPublicIDs.
	 *
	 * @param  ids             array of ChainPublicIDs.
	 * @return                 new PublicID.
	 * @throws VeyronException if the PublicID couldn't be created.
	 */
	public ChainPublicIDImpl(ChainPublicID[] ids) throws VeyronException {
		if (ids == null || ids.length == 0) {
			throw new VeyronException("Empty ids");
		}
		for (ChainPublicID id : ids) {
			if (id == null || id.getCertificates() == null || id.getCertificates().size() == 0) {
				throw new VeyronException("Empty certificate chain.");
			}
		}
		this.ids = ids;
		this.names = new String[ids.length];
		for (int i = 0; i < ids.length; ++i) {
			this.names[i] = getName(ids[i]);
		}
		this.key = getKey(ids[0]);
	}

	@Override
	public String[] names() {
		return this.names;
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
		return this.ids;
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
}