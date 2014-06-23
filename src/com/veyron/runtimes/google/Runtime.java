package com.veyron.runtimes.google;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import com.veyron2.Options;
import com.veyron2.OptionDefs;
import com.veyron2.ipc.Dispatcher;
import com.veyron2.ipc.VeyronException;

import org.joda.time.Duration;

import java.io.EOFException;
import java.util.Date;

/**
 * Runtime is the Veyron runtime that calls to native implementations for most of
 * its functionalities.
 */
public class Runtime implements com.veyron2.Runtime {
	private static Runtime globalRuntime = null;

	private static native void nativeGlobalInit();

	/**
	 * Returns the global runtime instance.
	 *
	 * @return Runtime a global runtime instance.
	 */
	public static synchronized Runtime global() {
		if (Runtime.globalRuntime == null) {
			try {
				Runtime.globalRuntime = new Runtime(false);
			} catch (VeyronException e) {
				throw new RuntimeException("Unexpected VeyronException: "+ e.getMessage());
			}
		}
		return Runtime.globalRuntime;
	}

	static {
		System.loadLibrary("jniwrapper");
		System.loadLibrary("veyronjni");
		nativeGlobalInit();
	}

	private final long nativePtr;
	private Client client = null;

	private native long nativeInit(boolean create) throws VeyronException;
	private native long nativeNewClient(long nativePtr, long timeoutMillis) throws VeyronException;
	private native long nativeNewServer(long nativePtr) throws VeyronException;
	private native long nativeGetClient(long nativePtr) throws VeyronException;
	private native long nativeNewContext(long nativePtr) throws VeyronException;
	private native void nativeFinalize(long nativePtr);

	public Runtime() throws VeyronException {
		this.nativePtr = nativeInit(true);
	}
	private Runtime(boolean create) throws VeyronException {
		this.nativePtr = nativeInit(create);
	}
	@Override
	public com.veyron2.ipc.Client newClient() throws VeyronException {
		return newClient(null);
	}
	@Override
	public com.veyron2.ipc.Client newClient(Options opts) throws VeyronException {
		final Duration timeout = opts.get(OptionDefs.CALL_TIMEOUT, Duration.class);
		final long nativeClientPtr =
			nativeNewClient(this.nativePtr, timeout != null ? timeout.getMillis() : -1);
		return new Client(nativeClientPtr);
	}
	@Override
	public com.veyron2.ipc.Server newServer() throws VeyronException {
		return newServer(null);
	}
	@Override
	public com.veyron2.ipc.Server newServer(Options opts) throws VeyronException {
		final long nativeServerPtr = nativeNewServer(this.nativePtr);
		return new Server(nativeServerPtr);
	}
	@Override
	public synchronized com.veyron2.ipc.Client getClient() {
		if (this.client == null) {
			try {
				final long nativeClientPtr = nativeGetClient(this.nativePtr);
				this.client = new Client(nativeClientPtr);
			} catch (VeyronException e) {
				throw new RuntimeException("Unexpected VeyronException: "+ e.getMessage());
			}
		}
		return this.client;
	}
	@Override
	public com.veyron2.ipc.Context newContext() {
		try {
			final long nativeContextPtr = nativeNewContext(this.nativePtr);
			return new Context(nativeContextPtr);
		} catch (VeyronException e) {
			throw new RuntimeException("Unexpected VeyronException: " + e.getMessage());
		}
	}
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}

	private static class Server implements com.veyron2.ipc.Server {
		private final long nativePtr;

		private native void nativeRegister(long nativePtr, String prefix, Dispatcher dispatcher)
			throws VeyronException;
		private native String nativeListen(long nativePtr, String protocol, String address)
			throws VeyronException;
		private native void nativePublish(long nativePtr, String name) throws VeyronException;
		private native void nativeStop(long nativePtr) throws VeyronException;
		private native void nativeFinalize(long nativePtr);

		Server(long nativePtr) {
			this.nativePtr = nativePtr;
		}

		@Override
		public void register(String prefix, com.veyron2.ipc.Dispatcher dispatcher)
			throws VeyronException {
			nativeRegister(this.nativePtr, prefix, dispatcher);
		}
		@Override
		public String listen(String protocol, String address) throws VeyronException {
			return nativeListen(this.nativePtr, protocol, address);
		}
		@Override
		public void publish(String name) throws VeyronException {
			nativePublish(this.nativePtr, name);
		}
		@Override
		public void stop() throws VeyronException {
			nativeStop(this.nativePtr);
		}
		@Override
		protected void finalize() {
			nativeFinalize(this.nativePtr);
		}
	}

	private static class Client implements com.veyron2.ipc.Client {
		private final long nativePtr;
		private final Gson gson;

		private native long nativeStartCall(long nativePtr, com.veyron2.ipc.Context context,
			String name, String method, String[] args, String idlPath, long timeoutMillis)
			throws VeyronException;
		private native void nativeClose(long nativePtr) throws VeyronException;
		private native void nativeFinalize(long nativePtr);

		Client(long nativePtr) {
			this.nativePtr = nativePtr;
			this.gson = new Gson();
		}
		@Override
		public Call startCall(com.veyron2.ipc.Context context, String name, String method,
			Object[] args) throws VeyronException {
			return startCall(context, name, method, args, null);
		}
		@Override
		public Call startCall(com.veyron2.ipc.Context context, String name, String method,
			Object[] args, Options opts) throws VeyronException {
			// Read options.
			final Duration timeout = opts.get(OptionDefs.CALL_TIMEOUT, Duration.class);
			final String vdlPath = opts.get(OptionDefs.VDL_INTERFACE_PATH, String.class);
			if (vdlPath == null) {
 				throw new VeyronException(String.format(
					"Must provide VDL interface path option for remote method %s on object %s",
					method, name));
			}

			// Encode all input arguments to JSON.
			final String[] jsonArgs = new String[args.length];
			for (int i = 0; i < args.length; i++) {
				jsonArgs[i] = this.gson.toJson(args[i]);
			}

			// Invoke native method.
			final long nativeCallPtr =
				nativeStartCall(this.nativePtr, context, name, method, jsonArgs, vdlPath,
					timeout != null ? timeout.getMillis() : -1);
			return new ClientCall(nativeCallPtr);
		}
		@Override
		public void close() {
			try {
				nativeClose(this.nativePtr);
			} catch (VeyronException e) {
				throw new RuntimeException("Unexpected VeyronException: " + e.getMessage());
			}
		}
		@Override
		protected void finalize() {
			nativeFinalize(this.nativePtr);
		}
	}

	private static class Context implements com.veyron2.ipc.Context {
		private final long nativePtr;

		private native void nativeFinalize(long nativePtr);

		Context(long nativePtr) {
			this.nativePtr = nativePtr;
		}

		@Override
		protected void finalize() {
			nativeFinalize(this.nativePtr);
		}
	}

	private static class Stream implements com.veyron2.ipc.Stream {
		private final long nativeStreamPtr;
		protected final Gson gson;

		private native void nativeSend(long nativeStreamPtr, String item) throws VeyronException;
		private native String nativeRecv(long nativeStreamPtr) throws EOFException, VeyronException;

		Stream(long nativeStreamPtr) {
			this.nativeStreamPtr = nativeStreamPtr;
			this.gson = new Gson();
		}

		@Override
		public void send(Object item) throws VeyronException {
			nativeSend(nativeStreamPtr, this.gson.toJson(item));
		}

		@Override
		public Object recv(TypeToken<?> type) throws EOFException, VeyronException {
			final String result = nativeRecv(nativeStreamPtr);
			try {
				return this.gson.fromJson(result, type.getType());
			} catch (JsonSyntaxException e) {
				throw new VeyronException(String.format(
					"Error decoding result %s from JSON: %s", result, e.getMessage()));
			}
		}
	}

	private static class ClientCall extends Stream implements com.veyron2.ipc.Client.Call {
		private final long nativePtr;

		private native String[] nativeFinish(long nativePtr) throws VeyronException;
		private native void nativeCancel(long nativePtr) throws VeyronException;
		private native void nativeFinalize(long nativePtr);

		ClientCall(long nativePtr) {
			super(nativePtr);
			this.nativePtr = nativePtr;
		}

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
				try {
					ret[i] = this.gson.fromJson(jsonResults[i], types[i].getType());
				} catch (JsonSyntaxException e) {
					throw new VeyronException(String.format(
						"Error decoding result %s from JSON: %s", jsonResults[i], e.getMessage()));
				}
			}
			return ret;
		}
		@Override
		public void cancel() {
			try {
				nativeCancel(this.nativePtr);
			} catch (VeyronException e) {
				throw new RuntimeException("Unexpected VeyronException: " + e.getMessage());
			}
		}
		@Override
		protected void finalize() {
			nativeFinalize(this.nativePtr);
		}
	}

	@SuppressWarnings("unused")
	private static class ServerCall extends Stream implements com.veyron2.ipc.ServerCall {
		private final long nativePtr;

		private native long nativeDeadline(long nativePtr) throws VeyronException;
		private native boolean nativeClosed(long nativePtr) throws VeyronException;
		private native void nativeFinalize(long nativePtr) throws VeyronException;

		public ServerCall(long nativePtr) {
			super(nativePtr);
			this.nativePtr = nativePtr;
		}
		@Override
		public Date deadline() {
			try {
				return new Date(nativeDeadline(this.nativePtr));
			} catch (VeyronException e) {
				throw new RuntimeException("Unexpected VeyronException: " + e.getMessage());
			}
		}
		@Override
		public boolean closed() {
			try {
				return nativeClosed(this.nativePtr);
			} catch (VeyronException e) {
				throw new RuntimeException("Unexpected VeyronException: " + e.getMessage());
			}
		}
		@Override
		protected void finalize() {
			try {
				nativeFinalize(this.nativePtr);
			} catch (VeyronException e) {
				throw new RuntimeException("Unexpected VeyronException: " + e.getMessage());
			}
		}
	}
}