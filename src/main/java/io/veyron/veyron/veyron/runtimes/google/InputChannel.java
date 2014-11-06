package io.veyron.veyron.veyron.runtimes.google;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.vdl.JSONUtil;

import java.io.EOFException;

public class InputChannel<T> implements io.veyron.veyron.veyron2.InputChannel<T> {
	private final long nativePtr;
	private final TypeToken<T> type;
	private final Gson gson;

	private native boolean nativeAvailable(long nativePtr);
	private native String nativeReadValue(long nativePtr) throws EOFException, VeyronException;
	private native void nativeFinalize(long nativePtr);

	/**
	 * Creates a new instance of InputChannel using the underlying Go channel as input.
	 * The Go channel must be of type {@code chan interface{}} *AND* it must be buffered.
	 *
	 * @param  nativePtr a pointer to the Go channel of type chan interface{}
	 * @param  type      type of Java values.
	 */
	public InputChannel(long nativePtr, TypeToken<T> type) {
		this.nativePtr = nativePtr;
		this.type = type;
		this.gson = JSONUtil.getGsonBuilder().create();
	}

	@Override
	public boolean available() {
		return nativeAvailable(this.nativePtr);
	}

	@Override
	public T readValue() throws EOFException, VeyronException {
		final String value = nativeReadValue(this.nativePtr);
		try {
			return this.gson.fromJson(value, this.type.getType());
		} catch (JsonSyntaxException e) {
			throw new VeyronException(String.format(
				"Illegal format for value of type %s: %s", type.toString(), value));
		}
	}
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}
