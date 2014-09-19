package com.veyron.runtimes.google.naming;

import com.google.gson.reflect.TypeToken;

import com.veyron.runtimes.google.InputChannel;
import com.veyron2.ipc.Context;
import com.veyron2.ipc.VeyronException;
import com.veyron2.naming.MountEntry;

public class Namespace implements com.veyron2.naming.Namespace {
	private final long nativePtr;

	// Returns the pointer to a *buffered* Go channel of type chan interface{}.
	private native long nativeGlob(
		long nativePtr, Context context, String pattern) throws VeyronException;
	private native void nativeFinalize(long nativePtr);

	public Namespace(long nativePtr) {
		this.nativePtr = nativePtr;
	}
	@Override
	public com.veyron2.InputChannel<MountEntry> glob(
			Context context, String pattern) throws VeyronException {
		final long chanPtr = nativeGlob(this.nativePtr, context, pattern);
		return new InputChannel(chanPtr, new TypeToken<MountEntry>(){});
	}
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}