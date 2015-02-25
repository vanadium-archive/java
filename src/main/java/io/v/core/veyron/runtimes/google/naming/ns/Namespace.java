package io.v.core.veyron.runtimes.google.naming.ns;

import com.google.common.reflect.TypeToken;

import io.v.v23.InputChannel;
import io.v.v23.verror.VException;
import io.v.v23.context.VContext;
import io.v.v23.naming.VDLMountEntry;

public class Namespace implements io.v.v23.naming.ns.Namespace {
    private final long nativePtr;

    private native InputChannel<VDLMountEntry> nativeGlob(
        long nativePtr, VContext context, String pattern) throws VException;
    private native void nativeFinalize(long nativePtr);

    public Namespace(long nativePtr) {
        this.nativePtr = nativePtr;
    }
    @Override
    public InputChannel<VDLMountEntry> glob(VContext context, String pattern) throws VException {
        return nativeGlob(this.nativePtr, context, pattern);
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