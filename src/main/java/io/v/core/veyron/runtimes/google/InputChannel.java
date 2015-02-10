package io.v.core.veyron.runtimes.google;

import io.v.core.veyron2.verror2.VException;
import io.v.core.veyron2.util.VomUtil;

import java.io.EOFException;
import java.lang.reflect.Type;

public class InputChannel<T> implements io.v.core.veyron2.InputChannel<T> {
	private final long nativePtr;
	private final Type type;

	private native boolean nativeAvailable(long nativePtr);
	private native byte[] nativeReadValue(long nativePtr) throws EOFException, VException;
	private native void nativeFinalize(long nativePtr);

	/**
	 * Creates a new instance of InputChannel using the underlying Go channel as input.
	 * The Go channel must be of type {@code chan interface{}} *AND* it must be buffered.
	 *
	 * @param  nativePtr a pointer to the Go channel of type chan interface{}
	 * @param  type      type of Java values.
	 */
	public InputChannel(long nativePtr, Type type) {
		this.nativePtr = nativePtr;
		this.type = type;
	}

	@Override
	public boolean available() {
		return nativeAvailable(this.nativePtr);
	}

	@Override
	public T readValue() throws EOFException, VException {
		final byte[] value = nativeReadValue(this.nativePtr);
		return (T) VomUtil.decode(value, this.type);
	}
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}