package com.veyron.runtimes.google.security;

import com.veyron2.ipc.VeyronException;
import com.veyron2.security.PrincipalPattern;

public class PublicIDStore implements com.veyron2.security.PublicIDStore {
	/**
	 * Params specifies options used for creating a new PublicIDStore.
	 */
	public class Params {
		/**
		 * Path to a directory in which a serialized PublicIDStore can be saved and loaded.
		 */
		public String dir;
		/**
		 * Signer used for generating and verifying signatures.
		 */
		public com.veyron2.security.Signer signer;
	}

	private static native long nativeCreate(Params params) throws VeyronException;

	/**
	 * Returns a new instance of PrivateIDStore based on params.
	 *  - If params is null, a new store with an empty set of PublicIDs and the default
	 *   pattern "*" (matched by all PublicIDs) is returned. The store only lives in
	 *   memory and is never persisted.
	 *  - If params is non-null, then a store obtained from the serialized data present
	 *   in params.Dir is returned if the data exists, or else a new store with an
	 *   empty set of PublicIDs and the default pattern "*" is returned. Any subsequent
	 *   modifications to the returned store are always signed (using params.Signer)
	 *   and persisted in params.Dir.
	 *
	 * @param  params          options used for creating a new PublicIDStore.
	 * @return                 new instance of PublicIDStore.
	 * @throws VeyronException if the PublicIDStore couldn't be created.
	 */
	public static PublicIDStore create(Params params) throws VeyronException {
		return new PublicIDStore(nativeCreate(params));
	}

	private final long nativePtr;

	private native void nativeAdd(long nativePtr, com.veyron2.security.PublicID id, String peerPattern)
		throws VeyronException;
	private native long nativeGetPeerID(long nativePtr, com.veyron2.security.PublicID peer)
		throws VeyronException;
	private native long nativeDefaultPublicID(long nativePtr) throws VeyronException;
	private native void nativeSetDefaultPrincipalPattern(long nativePtr, PrincipalPattern pattern)
		throws VeyronException;
	private native void nativeFinalize(long nativePtr);

	private PublicIDStore(long nativePtr) {
		this.nativePtr = nativePtr;
	}
	// Implements com.veyron2.security.PublicIDStore.
	@Override
	public void add(com.veyron2.security.PublicID id, PrincipalPattern peerPattern)
		throws VeyronException {
		nativeAdd(this.nativePtr, id, peerPattern.getValue());
	}
	@Override
	public com.veyron2.security.PublicID getPeerID(com.veyron2.security.PublicID peer)
		throws VeyronException {
		return new PublicID(nativeGetPeerID(this.nativePtr, peer));
	}
	@Override
	public com.veyron2.security.PublicID defaultPublicID() throws VeyronException {
		return new PublicID(nativeDefaultPublicID(this.nativePtr));
	}
	@Override
	public void setDefaultPrincipalPattern(PrincipalPattern pattern) throws VeyronException {
		nativeSetDefaultPrincipalPattern(this.nativePtr, pattern);
	}
	// Implements java.lang.Object.
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}