package com.veyron.runtimes.google.security;

public class Context implements com.veyron2.security.Context {
	private final long nativePtr;

	public native String nativeMethod(long nativePtr);
	private native String nativeName(long nativePtr);
	private native String nativeSuffix(long nativePtr);
	private native int nativeLabel(long nativePtr);
	private native long nativeLocalID(long nativePtr);
	private native long nativeRemoteID(long nativePtr);
	private native String nativeLocalEndpoint(long nativePtr);
	private native String nativeRemoteEndpoint(long nativePtr);
	private native void nativeFinalize(long nativePtr);

	public Context(long nativePtr) {
		this.nativePtr = nativePtr;
	}
	// Implements com.veyron2.security.Context.
	@Override
	public String method() {
		return nativeMethod(this.nativePtr);
	}
	@Override
	public String name() {
		return nativeName(this.nativePtr);
	}
	@Override
	public String suffix() {
		return nativeSuffix(this.nativePtr);
	}
	@Override
	public int label() {
		return nativeLabel(this.nativePtr);
	}
	@Override
	public com.veyron2.security.PublicID localID() {
		return new PublicID(nativeLocalID(this.nativePtr));
	}
	@Override
	public com.veyron2.security.PublicID remoteID() {
		return new PublicID(nativeRemoteID(this.nativePtr));
	}
	@Override
	public String localEndpoint() {
		return nativeLocalEndpoint(this.nativePtr);
	}
	@Override
	public String remoteEndpoint() {
		return nativeRemoteEndpoint(this.nativePtr);
	}
	// Implements java.lang.Object.
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}