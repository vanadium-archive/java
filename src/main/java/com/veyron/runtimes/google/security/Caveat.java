package com.veyron.runtimes.google.security;

import com.veyron2.ipc.VeyronException;
import com.veyron2.security.Context;

public class Caveat implements com.veyron2.security.Caveat {
	private final long nativePtr;

	public native void nativeValidate(long nativePtr, Context context) throws VeyronException;
	public native void nativeFinalize(long nativePtr);

	public Caveat(long nativePtr) {
		this.nativePtr = nativePtr;
	}
	// Implements com.veyron2.security.Caveat.
	@Override
	public void validate(Context context) throws VeyronException {
		nativeValidate(this.nativePtr, context);
	}
	// Implements java.lang.Object.
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}