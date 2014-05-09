package com.veyron.runtimes.google.ipc;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import com.veyron2.ipc.Dispatcher;
import com.veyron2.ipc.Options;
import com.veyron2.ipc.VeyronException;

import java.io.EOFException;

/**
 * Runtime is the Veyron runtime that calls to native implementations for most of
 * its functionalities.
 */
public class Runtime implements com.veyron2.ipc.Runtime {
	private static Client client = null;
	private static native void nativeInit();

	static {
		System.loadLibrary("ipcjni");
		nativeInit();
		try {
			client = new Client();
		} catch (VeyronException e) {
			System.out.println("error creating new client: " + e.getMessage());
			System.exit(1);
		}
	}

	@Override
	public com.veyron2.ipc.Client newClient(Object... opts) throws VeyronException {
		return new Client();
	}
	@Override
	public com.veyron2.ipc.Server newServer(Object... opts) throws VeyronException {
		return new Server();
	}
	@Override
	public com.veyron2.ipc.Client client() {
		return client;
	}

	private static class Server implements com.veyron2.ipc.Server {
		private final long nativeServerPtr;

		private native long nativeInit() throws VeyronException;
		private native void nativeRegister(long nativeServerPtr, String prefix, Dispatcher dispatcher) throws VeyronException;
		private native String nativeListen(long nativeServerPtr, String protocol, String address) throws VeyronException;
		private native void nativePublish(long nativeServerPtr, String name) throws VeyronException;
		private native void nativeStop(long nativeServerPtr) throws VeyronException;
		private native void nativeFinalize(long nativeServerPtr);

		/**
		 * Creates a new instance of the server.
		 *
		 * @throws VeyronException if the server couldn't be created
		 */
		Server() throws VeyronException {
			this.nativeServerPtr = nativeInit();
		}

		@Override
		public void register(String prefix, com.veyron2.ipc.Dispatcher dispatcher)
			throws VeyronException {
			nativeRegister(this.nativeServerPtr, prefix, dispatcher);
		}
		@Override
		public String listen(String protocol, String address) throws VeyronException {
			return nativeListen(this.nativeServerPtr, protocol, address);
		}
		@Override
		public void publish(String name) throws VeyronException {
			nativePublish(this.nativeServerPtr, name);
		}
		@Override
		public void stop() throws VeyronException {
			nativeStop(this.nativeServerPtr);
		}
		@Override
		protected void finalize() {
			nativeFinalize(this.nativeServerPtr);
		}
	}

	private static class Client implements com.veyron2.ipc.Client {
		private final long nativeClientPtr;
		private final Gson gson;

		private native long nativeInit() throws VeyronException;
		private native long nativeStartCall(long nativeClientPtr, String name, String method, String[] args, String idlPath, long timeoutMillis);
		private native void nativeClose(long nativeClientPtr);
		private native void nativeFinalize(long nativeClientPtr);

		Client() throws VeyronException {
			this.nativeClientPtr = nativeInit();
			this.gson = new Gson();
		}
		@Override
		public Call startCall(String name, String method, Object[] args, CallOption... opts) throws VeyronException {
			// Read options.
			String idlPath = null;
			long timeoutMillis = -1;  // default is no timeout.
			for (final CallOption opt : opts) {
				if (opt instanceof Options.IDLInterfacePath) {
					idlPath = ((Options.IDLInterfacePath)opt).getPath();
				} else if (opt instanceof Options.CallTimeout) {
					timeoutMillis = ((Options.CallTimeout)opt).getTimeout();
				}
			}
			if (idlPath == null) {
				throw new VeyronException(String.format(
					"Must provide IDL interface path option for remote method %s on object named %s",
					method, name));
			}

			// Encode all input arguments to JSON.
			final String[] jsonArgs = new String[args.length];
			for (int i = 0; i < args.length; i++) {
				jsonArgs[i] = this.gson.toJson(args[i]);
			}

			// Invoke native method.
			final long nativeClientCallPtr = nativeStartCall(this.nativeClientPtr, name, method,
				jsonArgs, idlPath, timeoutMillis);
			return new ClientCall(nativeClientCallPtr);
		}
		@Override
		public void close() {
			nativeClose(nativeClientPtr);
		}
		@Override
		protected void finalize() {
			nativeFinalize(this.nativeClientPtr);
		}
	}

	private static class ClientCall implements com.veyron2.ipc.Client.Call {
		private final long nativeClientCallPtr;
		private final Gson gson;

		private native String[] nativeFinish(long nativeClientCallPtr);
		private native void nativeCancel(long nativeClientCallPtr);
		private native void nativeFinalize(long nativeClientCallPtr);

		ClientCall(long nativeClientCallPtr) {
			this.nativeClientCallPtr = nativeClientCallPtr;
			this.gson = new Gson();
		}

		@Override
		public void send(Object item) throws VeyronException {
			// TODO(spetrovic): implement this.
		}

		@Override
		public Object recv(Class<?> type) throws EOFException, VeyronException {
			// TODO(spetrovic): implement this.
			return null;
		}

		@Override
		public void closeSend() throws VeyronException {
			// TODO(spetrovic): implement this.
		}
		@Override
		public Object[] finish(Class<?>[] types) throws VeyronException {
			// Call native method.
			final String[] jsonResults = nativeFinish(this.nativeClientCallPtr);
			if (jsonResults.length != types.length) {
				throw new VeyronException(String.format(
					"Mismatch in number of results, want %s, have %s",
					types.length, jsonResults.length));
			}

			// JSON-decode results and return.
			final Object[] ret = new Object[types.length];
			for (int i = 0; i < types.length; i++) {
				try {
					ret[i] = this.gson.fromJson(jsonResults[i], types[i]);
				} catch (JsonSyntaxException e) {
					throw new VeyronException(String.format(
						"Error decoding result %s from JSON: %s", jsonResults[i], e.getMessage()));
				}
			}
			return ret;
		}
		@Override
		public void cancel() {
			nativeCancel(this.nativeClientCallPtr);
		}
		@Override
		protected void finalize() {
			nativeFinalize(this.nativeClientCallPtr);
		}
	}
}