package io.veyron.veyron.veyron.runtimes.google.security;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.security.CryptoUtil;
import io.veyron.veyron.veyron2.security.wire.ChainPublicID;
import io.veyron.veyron.veyron2.vdl.JSONUtil;

import java.security.interfaces.ECPublicKey;

public class PublicID implements io.veyron.veyron.veyron2.security.PublicID {
	private final long nativePtr;

	private native String[] nativeNames(long nativePtr);
	private native byte[] nativePublicKey(long nativePtr) throws VeyronException;
	private native long nativeAuthorize(long nativePtr, io.veyron.veyron.veyron2.security.Context context)
		throws VeyronException;
	private native String[] nativeEncode(long nativePtr) throws VeyronException;
	private native boolean nativeEquals(long nativePtr, long otherNativePtr);
	private native void nativeFinalize(long nativePtr);

	public PublicID(long nativePtr) {
		this.nativePtr = nativePtr;
	}
	// Implements io.veyron.veyron.veyron2.security.PublicID.
	@Override
	public String[] names() {
		return nativeNames(this.nativePtr);
	}
	@Override
	public ECPublicKey publicKey() {
		try {
			final byte[] encodedKey = this.nativePublicKey(this.nativePtr);
			return CryptoUtil.decodeECPublicKey(encodedKey);
		} catch (VeyronException e) {
			throw new RuntimeException(
				"Couldn't decode native ECDSA public key: " + e.getMessage());
		}
	}
	@Override
	public io.veyron.veyron.veyron2.security.PublicID authorize(io.veyron.veyron.veyron2.security.Context context)
		throws VeyronException {
		return new PublicID(nativeAuthorize(this.nativePtr, context));
	}
	@Override
	public ChainPublicID[] encode() throws VeyronException {
		final Gson gson = JSONUtil.getGsonBuilder().create();
		final String[] chains = nativeEncode(this.nativePtr);
		final ChainPublicID[] ret = new ChainPublicID[chains.length];
		for (int i = 0; i < chains.length; ++i) {
			try {
				ret[i] = (ChainPublicID) gson.fromJson(
					chains[i], new TypeToken<ChainPublicID>(){}.getType());
			} catch (JsonSyntaxException e) {
				throw new VeyronException("Couldn't JSON decode chain: " + chains[i]);
			}
		}
		return ret;
	}
	// Implements java.lang.Object.
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof PublicID)) return false;
		final PublicID other = (PublicID)obj;
		return nativeEquals(this.nativePtr, other.nativePtr);
	}
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
	private long getNativePtr() { return this.nativePtr; }
}
