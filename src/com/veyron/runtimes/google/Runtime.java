package com.veyron.runtimes.google;

import java.io.EOFException;
import java.util.Date;

import org.joda.time.Duration;

import com.google.common.reflect.TypeToken;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import com.veyron2.OptionDefs;
import com.veyron2.Options;
import com.veyron2.ipc.Dispatcher;
import com.veyron2.ipc.VeyronException;
import com.veyron2.security.PublicID;

/**
 * Runtime is the Veyron runtime that calls to native implementations for most of
 * its functionalities.
 */
public class Runtime implements com.veyron2.Runtime {
	private static Runtime globalRuntime = null;

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
	}

	private final long nativePtr;
	private Client client = null;

	private native long nativeInit(boolean create) throws VeyronException;
	private native long nativeNewClient(long nativePtr, long timeoutMillis) throws VeyronException;
	private native long nativeNewServer(long nativePtr) throws VeyronException;
	private native long nativeGetClient(long nativePtr);
	private native long nativeNewContext(long nativePtr);
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
			final long nativeClientPtr = nativeGetClient(this.nativePtr);
			this.client = new Client(nativeClientPtr);
		}
		return this.client;
	}
	@Override
	public com.veyron2.ipc.Context newContext() {
		final long nativeContextPtr = nativeNewContext(this.nativePtr);
		return new Context(nativeContextPtr);
	}
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}

	private static class Server implements com.veyron2.ipc.Server {
		private final long nativePtr;

		private native String nativeListen(long nativePtr, String protocol, String address)
			throws VeyronException;
		private native void nativeServe(long nativePtr, String name, Dispatcher dispatcher)
			throws VeyronException;
		private native String[] nativeGetPublishedNames(long nativePtr) throws VeyronException;
		private native void nativeStop(long nativePtr) throws VeyronException;
		private native void nativeFinalize(long nativePtr);

		Server(long nativePtr) {
			this.nativePtr = nativePtr;
		}
		// Implement com.veyron2.ipc.Server.
		@Override
		public String listen(String protocol, String address) throws VeyronException {
			return nativeListen(this.nativePtr, protocol, address);
		}
		@Override
		public void serve(String name, Dispatcher dispatcher) throws VeyronException {
			nativeServe(this.nativePtr, name, dispatcher);
		}
		@Override
		public String[] getPublishedNames() throws VeyronException {
			return nativeGetPublishedNames(this.nativePtr);
		}
		@Override
		public void stop() throws VeyronException {
			nativeStop(this.nativePtr);
		}
		// Implement java.lang.Object.
		@Override
		protected void finalize() {
			nativeFinalize(this.nativePtr);
		}
	}

	private static class Client implements com.veyron2.ipc.Client {
		private final long nativePtr;
		// TODO(bprosnitz) Ensure gson is thread safe.
		private final Gson gson;

		private native long nativeStartCall(long nativePtr, com.veyron2.ipc.Context context,
			String name, String method, String[] args, String idlPath, long timeoutMillis)
			throws VeyronException;
		private native void nativeClose(long nativePtr);
		private native void nativeFinalize(long nativePtr);

		Client(long nativePtr) {
			this.nativePtr = nativePtr;
			// TODO(bprosnitz) This case conversion should be done in VOM like we do for javascript.
            this.gson =
            	new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
		}
		// Implement com.veyron2.ipc.Client.
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
			nativeClose(this.nativePtr);
		}
		// Implement java.lang.Object.
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
		// Implement java.lang.Object.
		@Override
		protected void finalize() {
			nativeFinalize(this.nativePtr);
		}
	}

	private static class Stream implements com.veyron2.ipc.Stream {
		private final long nativeStreamPtr;
		private final Gson gson;

		private native void nativeSend(long nativeStreamPtr, String item) throws VeyronException;
		private native String nativeRecv(long nativeStreamPtr) throws EOFException, VeyronException;

		Stream(long nativeStreamPtr) {
			this.nativeStreamPtr = nativeStreamPtr;
			this.gson =
				new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
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
		private final Gson gson;

		private native String[] nativeFinish(long nativePtr) throws VeyronException;
		private native void nativeCancel(long nativePtr);
		private native void nativeFinalize(long nativePtr);

		ClientCall(long nativePtr) {
			super(nativePtr);
			this.nativePtr = nativePtr;
			this.gson =
				new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
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
			nativeCancel(this.nativePtr);
		}
		@Override
		protected void finalize() {
			nativeFinalize(this.nativePtr);
		}
	}

	@SuppressWarnings("unused")
	private static class ServerCall extends Stream implements com.veyron2.ipc.ServerCall {
		private final long nativePtr;
		private final com.veyron.runtimes.google.security.Context context;

		public native long nativeBlessing(long nativePtr);
		private native long nativeDeadline(long nativePtr);
		private native boolean nativeClosed(long nativePtr);

		public ServerCall(long nativePtr) {
			super(nativePtr);
			this.nativePtr = nativePtr;
			this.context = new com.veyron.runtimes.google.security.Context(this.nativePtr);
		}
		// Implements com.veyron2.ipc.ServerContext.
		@Override
		public com.veyron2.security.PublicID blessing() {
			return new com.veyron.runtimes.google.security.PublicID(nativeBlessing(this.nativePtr));
		}
		@Override
		public Date deadline() {
			return new Date(nativeDeadline(this.nativePtr));
		}
		@Override
		public boolean closed() {
			return nativeClosed(this.nativePtr);
		}
		// Implements com.veyron2.security.Context.
		@Override
		public String method() {
			return this.context.method();
		}
		@Override
		public String name() {
			return this.context.name();
		}
		@Override
		public String suffix() {
			return this.context.suffix();
		}
		@Override
		public int label() {
			return this.context.label();
		}
		@Override
		public PublicID localID() {
			return this.context.localID();
		}
		@Override
		public PublicID remoteID() {
			return this.context.remoteID();
		}
		@Override
		public String localEndpoint() {
			return this.context.localEndpoint();
		}
		@Override
		public String remoteEndpoint() {
			return this.context.remoteEndpoint();
		}
	}
}