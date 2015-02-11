package io.v.core.veyron2.security;

import io.v.core.veyron2.verror.VException;

import java.security.interfaces.ECPublicKey;

class BlessingsImpl extends Blessings {
	private static final String TAG = "Veyron runtime";

	private static native Blessings nativeCreate(WireBlessings wire) throws VException;
	private static native Blessings nativeCreateUnion(Blessings[] blessings) throws VException;

	static Blessings create(WireBlessings wire) throws VException {
		return nativeCreate(wire);
	}

	static Blessings createUnion(Blessings... blessings) throws VException {
		return nativeCreateUnion(blessings);
	}

	private final long nativePtr;
	private final WireBlessings wire;  // non-null

	private native String[] nativeForContext(long nativePtr, VContext context)
		throws VException;
	private native ECPublicKey nativePublicKey(long nativePtr) throws VException;
	private native void nativeFinalize(long nativePtr);

	private BlessingsImpl(long nativePtr, WireBlessings wire) {
		this.nativePtr = nativePtr;
		this.wire = wire;
	}

	@Override
	public String[] forContext(VContext context) {
		try {
			return nativeForContext(this.nativePtr, context);
		} catch (VException e) {
			android.util.Log.e(TAG, "Couldn't get blessings for context: " + e.getMessage());
			return null;
		}
	}
	@Override
	public ECPublicKey publicKey() {
		try {
			return nativePublicKey(this.nativePtr);
		} catch (VException e) {
			android.util.Log.e(TAG, "Coudln't get public key: " + e.getMessage());
			return null;
		}
	}
	@Override
	public WireBlessings wireFormat() {
		return this.wire;
	}
	@Override
	void implementationsOnlyInThisPackage() {
	}
	@Override
	public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof Blessings)) return false;
        final Blessings other = (Blessings) obj;
        return this.wireFormat().equals(other.wireFormat());
    }
    @Override
    public int hashCode() {
    	return this.wire.hashCode();
    }
	@Override
	public String toString() {
		return this.wire.toString();
	}
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}