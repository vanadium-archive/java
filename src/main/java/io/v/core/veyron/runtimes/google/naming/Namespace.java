package io.v.core.veyron.runtimes.google.naming;

import com.google.common.reflect.TypeToken;

import io.v.core.veyron.runtimes.google.InputChannel;
import io.v.core.veyron2.VeyronException;
import io.v.core.veyron2.context.VContext;
import io.v.core.veyron2.naming.VDLMountEntry;

public class Namespace implements io.v.core.veyron2.naming.Namespace {
	private final long nativePtr;

	// Returns the pointer to a *buffered* Go channel of type chan interface{}.
	private native long nativeGlob(
		long nativePtr, VContext context, String pattern) throws VeyronException;
	private native void nativeFinalize(long nativePtr);

	public Namespace(long nativePtr) {
		this.nativePtr = nativePtr;
	}
	@Override
	public io.v.core.veyron2.InputChannel<VDLMountEntry> glob(VContext context, String pattern)
		throws VeyronException {
		final long chanPtr = nativeGlob(this.nativePtr, context, pattern);
		return new InputChannel<VDLMountEntry>(chanPtr, new TypeToken<VDLMountEntry>(){}.getType());
	}
	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (other == null) return false;
		if (this.getClass() != other.getClass()) return false;
		return this.nativePtr == ((Namespace) other).nativePtr;
	}
	@Override
	public int hashCode() {
		return Long.valueOf(this.nativePtr).hashCode();
	}
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}