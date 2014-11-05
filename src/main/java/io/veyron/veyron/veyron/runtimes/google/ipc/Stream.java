package io.veyron.veyron.veyron.runtimes.google.ipc;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.vdl.JSONUtil;

import java.io.EOFException;

public class Stream implements io.veyron.veyron.veyron2.ipc.Stream {
	private final long nativePtr;
	private final Gson gson;

	private native void nativeSend(long nativePtr, String item) throws VeyronException;
	private native String nativeRecv(long nativePtr) throws EOFException, VeyronException;
	private native void nativeFinalize(long nativePtr);

	private Stream(long nativePtr) {
		this.nativePtr = nativePtr;
		this.gson = JSONUtil.getGsonBuilder().create();
	}
	@Override
	public void send(Object item) throws VeyronException {
		nativeSend(nativePtr, this.gson.toJson(item));
	}

	@Override
	public Object recv(TypeToken<?> type) throws EOFException, VeyronException {
		final String result = nativeRecv(nativePtr);
		try {
			return this.gson.fromJson(result, type.getType());
		} catch (JsonSyntaxException e) {
			throw new VeyronException(String.format(
				"Error decoding result %s from JSON: %s", result, e.getMessage()));
		}
	}
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}
