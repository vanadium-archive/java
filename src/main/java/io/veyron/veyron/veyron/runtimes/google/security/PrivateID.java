package io.veyron.veyron.veyron.runtimes.google.security;


import com.google.gson.Gson;

import org.joda.time.Duration;

import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.security.Signature;
import io.veyron.veyron.veyron2.security.wire.ChainPublicID;
import io.veyron.veyron.veyron2.vdl.JSONUtil;

import java.security.interfaces.ECPublicKey;

public class PrivateID implements io.veyron.veyron.veyron2.security.PrivateID {
	private static native long nativeCreate(
		String name, io.veyron.veyron.veyron2.security.Signer signer) throws VeyronException;

	/**
	 * Returns a new PrivateID that uses the provided Signer to generate signatures.  The returned
	 * PrivateID contains a single self-signed certificate with the given name.
	 *
	 * @param  name            a name specified in the certificate, e.g., Alice, Bob.
	 * @param  signer          a signer used for generating signatures.
	 * @return                 the private id.
	 * @throws VeyronException if the private id couldn't be created.
	 */
	public static PrivateID create(
		String name, io.veyron.veyron.veyron2.security.Signer signer) throws VeyronException {
		return new PrivateID(nativeCreate(name, signer), signer);
	}

	private final long nativePtr;
	private final io.veyron.veyron.veyron2.security.Signer signer;

	private native long nativePublicID(long nativePtr);
	private native long nativeBless(long nativePtr, String[] blessee, String blessingName,
		Duration duration) throws VeyronException;
	private native long nativeDerive(long nativePtr, String[] publicID) throws VeyronException;
	private native void nativeFinalize(long nativePtr);

	public PrivateID(long nativePtr, io.veyron.veyron.veyron2.security.Signer signer) {
		this.nativePtr = nativePtr;
		this.signer = signer;
	}
	// Implements io.veyron.veyron.veyron2.security.PrivateID.
	@Override
	public io.veyron.veyron.veyron2.security.PublicID publicID() {
		return new PublicID(nativePublicID(this.nativePtr));
	}
	@Override
	public Signature sign(byte[] message) throws VeyronException {
		return this.signer.sign(null, message);
	}
	@Override
	public ECPublicKey publicKey() {
		return this.signer.publicKey();
	}
	@Override
	public io.veyron.veyron.veyron2.security.PublicID bless(
		io.veyron.veyron.veyron2.security.PublicID blessee, String blessingName, Duration duration)
		throws VeyronException {
		final String[] chainIDs = Util.encodeChains(blessee.encode());
		return new PublicID(nativeBless(this.nativePtr, chainIDs, blessingName, duration));
	}
	@Override
	public io.veyron.veyron.veyron2.security.PrivateID derive(
		io.veyron.veyron.veyron2.security.PublicID publicID) throws VeyronException {
		final String[] chainIDs = Util.encodeChains(publicID.encode());
		return new PrivateID(nativeDerive(this.nativePtr, chainIDs), this.signer);
	}
	// Implements java.lang.Object.
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}
