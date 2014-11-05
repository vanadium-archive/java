package io.veyron.veyron.veyron.runtimes.google.ipc;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.vdl.Any;
import io.veyron.veyron.veyron2.vdl.JSONUtil;

import java.io.EOFException;

public class ClientCall implements io.veyron.veyron.veyron2.ipc.Client.Call {
	private final long nativePtr;
	private final Stream stream;
	private final Gson gson;

	private native String[] nativeFinish(long nativePtr) throws VeyronException;
	private native void nativeCancel(long nativePtr);
	private native void nativeFinalize(long nativePtr);

	private ClientCall(long nativePtr, Stream stream) {
		this.nativePtr = nativePtr;
		this.stream = stream;
		this.gson = JSONUtil.getGsonBuilder().create();
	}

	// Implements io.veyron.veyron.veyron2.ipc.Client.Call.
	@Override
	public void closeSend() throws VeyronException {
		// TODO(spetrovic): implement this.
	}
	@Override
	public Object[] finish(TypeToken<?>[] types) throws VeyronException {
		// Call native method.
		final String[] jsonResults = nativeFinish(this.nativePtr);
		if (jsonResults.length != types.length) {
			throw new VeyronException(String.format(
				"Mismatch in number of results, want %s, have %s",
				types.length, jsonResults.length));
		}

		// JSON-decode results and return.
		final Object[] ret = new Object[types.length];
		for (int i = 0; i < types.length; i++) {
			final TypeToken<?> type = types[i];
			final String jsonResult = jsonResults[i];
			if (type.equals(new TypeToken<Any>(){})) {  // Any type.
				ret[i] = new Any(jsonResult);
				continue;
			}
			try {
				ret[i] = this.gson.fromJson(jsonResult, type.getType());
			} catch (JsonSyntaxException e) {
				throw new VeyronException(String.format(
					"Error decoding JSON result %s into type %s: %s",
					jsonResult, e.getMessage()));
			}
		}
		return ret;
	}
	@Override
	public void cancel() {
		nativeCancel(this.nativePtr);
	}
	// Implements io.veyron.veyron.veyron2.ipc.Stream.
	@Override
	public void send(Object item) throws VeyronException {
		this.stream.send(item);
	}
	@Override
	public Object recv(TypeToken<?> type) throws EOFException, VeyronException {
		return this.stream.recv(type);
	}
	// Implements java.lang.Object.
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}