package io.veyron.veyron.veyron.runtimes.google.security;

import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.security.BlessingPattern;

public class PublicIDStore implements io.veyron.veyron.veyron2.security.PublicIDStore {
	private final long nativePtr;

	private native void nativeAdd(long nativePtr, io.veyron.veyron.veyron2.security.PublicID id, String peerPattern)
		throws VeyronException;
	private native long nativeGetPeerID(long nativePtr, io.veyron.veyron.veyron2.security.PublicID peer)
		throws VeyronException;
	private native long nativeDefaultPublicID(long nativePtr) throws VeyronException;
	private native void nativeSetDefaultBlessingPattern(long nativePtr, BlessingPattern pattern)
		throws VeyronException;
	private native void nativeFinalize(long nativePtr);

	public PublicIDStore(long nativePtr) {
		this.nativePtr = nativePtr;
	}
	// Implements io.veyron.veyron.veyron2.security.PublicIDStore.
	@Override
	public void add(io.veyron.veyron.veyron2.security.PublicID id, BlessingPattern peerPattern)
		throws VeyronException {
		nativeAdd(this.nativePtr, id, peerPattern.getValue());
	}
	@Override
	public io.veyron.veyron.veyron2.security.PublicID getPeerID(io.veyron.veyron.veyron2.security.PublicID peer)
		throws VeyronException {
		return new PublicID(nativeGetPeerID(this.nativePtr, peer));
	}
	@Override
	public io.veyron.veyron.veyron2.security.PublicID defaultPublicID() throws VeyronException {
		return new PublicID(nativeDefaultPublicID(this.nativePtr));
	}
	@Override
	public void setDefaultBlessingPattern(BlessingPattern pattern) throws VeyronException {
		nativeSetDefaultBlessingPattern(this.nativePtr, pattern);
	}
	// Implements java.lang.Object.
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}
