package com.veyron.runtimes.google.security;

import com.veyron2.ipc.VeyronException;
import com.veyron2.security.ServiceCaveat;

import org.joda.time.Duration;

public class PrivateID implements com.veyron2.security.PrivateID {
	private static native long nativeCreate(String name) throws VeyronException;

	/**
	 * Returns a new PrivateID containing a freshly generated private key, a single self-signed
	 * certificate specifying the provided name, and the public key corresponding to the generated
	 * private key.
	 *
	 * @param  name            a name specified in the certificate, e.g., Alice, Bob.
	 * @return                 the private id.
	 * @throws VeyronException if the private id couldn't be created.
	 */
	public static PrivateID create(String name) throws VeyronException {
		return new PrivateID(nativeCreate(name));
	}

	private final long nativePtr;

	private native long nativePublicID(long nativePtr);
	private native byte[] nativeSign(long nativePtr, byte[] message) throws VeyronException;
	private native long nativeBless(long nativePtr, com.veyron2.security.PublicID blessee,
		String blessingName, long durationMilliseconds, ServiceCaveat[] caveats)
		throws VeyronException;
	private native long nativeDerive(long nativePtr, com.veyron2.security.PublicID publicID)
		throws VeyronException;
	private native void nativeFinalize(long nativePtr);

	public PrivateID(long nativePtr) {
		this.nativePtr = nativePtr;
	}
	// Implements com.veyron2.security.PrivateID.
	@Override
	public com.veyron2.security.PublicID publicID() {
		return new PublicID(nativePublicID(this.nativePtr));
	}
	@Override
	public byte[] sign(byte[] message) throws VeyronException {
		return nativeSign(this.nativePtr, message);
	}
	@Override
	public com.veyron2.security.PublicID bless(com.veyron2.security.PublicID blessee,
		String blessingName, Duration duration,	ServiceCaveat[] caveats) throws VeyronException {
		return new PublicID(
			nativeBless(this.nativePtr, blessee, blessingName, duration.getMillis(), caveats));
	}
	@Override
	public com.veyron2.security.PrivateID derive(com.veyron2.security.PublicID publicID)
		throws VeyronException {
		return new PrivateID(nativeDerive(this.nativePtr, publicID));
	}
	// Implements java.lang.Object.
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}
