package io.veyron.veyron.veyron.runtimes.google.ipc;

import com.google.gson.Gson;

import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.context.Context;
import io.veyron.veyron.veyron2.ipc.VeyronException;
import io.veyron.veyron.veyron2.vdl.JSONUtil;

public class Client implements io.veyron.veyron.veyron2.ipc.Client {
	private final long nativePtr;
	private final Gson gson;

	private native ClientCall nativeStartCall(long nativePtr, Context context, String name,
		String method, String[] args, Options opts) throws VeyronException;
	private native void nativeClose(long nativePtr);
	private native void nativeFinalize(long nativePtr);

	private Client(long nativePtr) {
		this.nativePtr = nativePtr;
		this.gson = JSONUtil.getGsonBuilder().create();
	}
	// Implement io.veyron.veyron.veyron2.ipc.Client.
	@Override
	public Call startCall(Context context, String name, String method, Object[] args)
		throws VeyronException {
		return startCall(context, name, method, args, null);
	}
	@Override
	public Call startCall(Context context, String name, String method, Object[] args,
		Options opts) throws VeyronException {
		if (method == "") {
			throw new VeyronException("Empty method name invoked on object %s", name);
		}

		// Encode all input arguments to JSON.
		final String[] jsonArgs = new String[args.length];
		for (int i = 0; i < args.length; i++) {
			jsonArgs[i] = this.gson.toJson(args[i]);
		}

		// Invoke native method.
		// Make sure that the method name starts with an uppercase character.
		method = Character.toUpperCase(method.charAt(0)) + method.substring(1);
		return nativeStartCall(this.nativePtr, context, name, method, jsonArgs, opts);
	}
	@Override
	public void close() {
		nativeClose(this.nativePtr);
	}
	// Implement java.lang.Object.
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}