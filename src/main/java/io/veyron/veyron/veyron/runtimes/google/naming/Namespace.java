package io.veyron.veyron.veyron.runtimes.google.naming;

import com.google.gson.reflect.TypeToken;

import io.veyron.veyron.veyron.runtimes.google.InputChannel;
import io.veyron.veyron.veyron2.ipc.Context;
import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.naming.MountEntry;

public class Namespace implements io.veyron.veyron.veyron2.naming.Namespace {
	private final long nativePtr;

	// Returns the pointer to a *buffered* Go channel of type chan interface{}.
	private native long nativeGlob(
		long nativePtr, Context context, String pattern) throws VeyronException;
	private native void nativeFinalize(long nativePtr);

	public Namespace(long nativePtr) {
		this.nativePtr = nativePtr;
	}
	@Override
	public io.veyron.veyron.veyron2.InputChannel<MountEntry> glob(
			Context context, String pattern) throws VeyronException {
		final long chanPtr = nativeGlob(this.nativePtr, context, pattern);
		return new InputChannel(chanPtr, new TypeToken<MountEntry>(){});
	}
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}