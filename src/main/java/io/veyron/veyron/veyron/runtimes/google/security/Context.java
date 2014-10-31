package io.veyron.veyron.veyron.runtimes.google.security;

import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.security.Blessings;
import io.veyron.veyron.veyron2.security.Label;
import io.veyron.veyron.veyron2.security.Principal;

public class Context implements io.veyron.veyron.veyron2.security.Context {
	private static final String TAG = "Veyron runtime";

	private final long nativePtr;

	public native String nativeMethod(long nativePtr);
	private native String nativeName(long nativePtr);
	private native String nativeSuffix(long nativePtr);
	private native int nativeLabel(long nativePtr);
	private native String nativeLocalEndpoint(long nativePtr);
	private native String nativeRemoteEndpoint(long nativePtr);
	private native Principal nativeLocalPrincipal(long nativePtr) throws VeyronException;
	private native Blessings nativeLocalBlessings(long nativePtr) throws VeyronException;
	private native Blessings nativeRemoteBlessings(long nativePtr) throws VeyronException;
	private native void nativeFinalize(long nativePtr);

	public Context(long nativePtr) {
		this.nativePtr = nativePtr;
	}
	// Implements io.veyron.veyron.veyron2.security.Context.
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
	public Label label() {
		return new Label(nativeLabel(this.nativePtr));
	}
	@Override
	public String localEndpoint() {
		return nativeLocalEndpoint(this.nativePtr);
	}
	@Override
	public String remoteEndpoint() {
		return nativeRemoteEndpoint(this.nativePtr);
	}
	@Override
	public Principal localPrincipal() {
		try {
			return nativeLocalPrincipal(this.nativePtr);
		} catch (VeyronException e) {
			android.util.Log.e(TAG, "Couldn't get local Principal: " + e.getMessage());
			return null;
		}
	}
	@Override
	public Blessings localBlessings() {
		try {
			return nativeLocalBlessings(this.nativePtr);
		} catch (VeyronException e) {
			android.util.Log.e(TAG, "Couldn't get local Blessings: " + e.getMessage());
			return null;
		}
	}
	@Override
	public Blessings remoteBlessings() {
		try {
			return nativeRemoteBlessings(this.nativePtr);
		} catch (VeyronException e) {
			android.util.Log.e(TAG, "Couldn't get remote Blessings: " + e.getMessage());
			return null;
		}
	}
	// Implements java.lang.Object.
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}