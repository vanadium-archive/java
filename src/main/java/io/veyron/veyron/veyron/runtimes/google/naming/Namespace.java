package io.veyron.veyron.veyron.runtimes.google.naming;

import com.google.common.reflect.TypeToken;

import io.veyron.veyron.veyron.runtimes.google.InputChannel;
import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.context.Context;
import io.veyron.veyron.veyron2.naming.VDLMountEntry;

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
	public io.veyron.veyron.veyron2.InputChannel<VDLMountEntry> glob(Context context, String pattern)
		throws VeyronException {
		final long chanPtr = nativeGlob(this.nativePtr, context, pattern);
		return new InputChannel<VDLMountEntry>(chanPtr, new TypeToken<VDLMountEntry>(){}.getType());
	}
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}