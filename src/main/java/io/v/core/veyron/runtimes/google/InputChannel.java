package io.v.core.veyron.runtimes.google;

import io.v.v23.verror.VException;
import java.io.EOFException;

public class InputChannel<T> implements io.v.v23.InputChannel<T> {
    private final long nativePtr;
    private final long sourceNativePtr;

    private native boolean nativeAvailable(long nativePtr);
    private native Object nativeReadValue(long nativePtr) throws EOFException, VException;
    private native void nativeFinalize(long nativePtr, long sourceNativePtr);

    /**
     * Creates a new instance of {@code InputChannel}.
     *
     * @param  nativePtr       native pointer to the Go channel of Java objects
     * @param  sourceNativePtr native pointer to the Go channel that feeds the above Go channel
     *                         of Java object
     */
    public InputChannel(long nativePtr, long sourceNativePtr) {
        this.nativePtr = nativePtr;
        this.sourceNativePtr = sourceNativePtr;
    }

    @Override
    public boolean available() {
        return nativeAvailable(this.nativePtr);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T readValue() throws EOFException, VException {
        return (T) nativeReadValue(this.nativePtr);
    }
    @Override
    protected void finalize() {
        nativeFinalize(this.nativePtr, this.sourceNativePtr);
    }
    /**
     * Returns the native pointer to the Go channel of Java objects.
     *
     * @return the native pointer to the Go channel of Java objects
     */
    public long getNativePtr() { return this.nativePtr; }

    /**
     * Returns the native pointer to the Go channel that feeds the above Go channel of Java object.
     *
     * @return the native pointer to the Go channel that feeds the above Go channel of Java object
     */
    public long getSourceNativePtr() { return this.sourceNativePtr; }
}