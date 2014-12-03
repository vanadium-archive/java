package io.veyron.veyron.veyron.runtimes.google.ipc;

import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.context.Context;

import java.lang.reflect.Type;

public class Client implements io.veyron.veyron.veyron2.ipc.Client {
	private final long nativePtr;

	private native io.veyron.veyron.veyron2.ipc.Client.Call nativeStartCall(long nativePtr,
		Context context, String name, String method, byte[][] vomArgs, Options opts)
		throws VeyronException;
	private native void nativeClose(long nativePtr);
	private native void nativeFinalize(long nativePtr);

	private Client(long nativePtr) {
		this.nativePtr = nativePtr;
	}
	// Implement io.veyron.veyron.veyron2.ipc.Client.
	@Override
	public io.veyron.veyron.veyron2.ipc.Client.Call startCall(Context context, String name,
	        String method, Object[] args, Type[] argTypes) throws VeyronException {
		return startCall(context, name, method, args, argTypes, null);
	}
	@Override
	public io.veyron.veyron.veyron2.ipc.Client.Call startCall(Context context, String name,
	        String method, Object[] args, Type[] argTypes, Options opts) throws VeyronException {
		if (opts == null) {
			opts = new Options();
		}
		if (method == "") {
			throw new VeyronException("Empty method name invoked on object %s", name);
		}
		if (args.length != argTypes.length) {
			throw new VeyronException(String.format(
			        "Argument count (%d) doesn't match type count (%d) for method %s of object %s",
			        args.length, argTypes.length, name, method));
		}
		// VOM-encode all input arguments, individually.
		final byte[][] vomArgs = new byte[args.length][];
		for (int i = 0; i < args.length; ++i) {
			vomArgs[i] = Util.VomEncode(args[i], argTypes[i]);
		}

		// Invoke native method, making sure that the method name starts with an
		// upper case character.
		method = Character.toUpperCase(method.charAt(0)) + method.substring(1);
		return nativeStartCall(this.nativePtr, context, name, method, vomArgs, opts);
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