package com.veyron.runtimes.google.ipc;

import com.veyron2.ipc.Dispatcher;
import com.veyron2.ipc.VeyronException;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime is the Veyron runtime that calls to native implementations for most of
 * its functionalities.
 */
public class Runtime implements com.veyron2.ipc.Runtime {
	private static final Client client = new Client();

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
		private final Set<Dispatcher> dispatchers;  // must be thread-safe

		private native long nativeInit() throws VeyronException;
		private native void nativeRegister(long nativeServerPtr, String prefix, Dispatcher dispatcher) throws VeyronException;
		private native String nativeListen(long nativeServerPtr, String protocol, String address) throws VeyronException;
		private native void nativePublish(long nativeServerPtr, String name) throws VeyronException;
		private native void nativeStop(long nativeServerPtr) throws VeyronException;

		/**
		 * Creates a new instance of the server.
		 *
		 * @throws VeyronException if the server couldn't be created
		 */
		public Server() throws VeyronException {
			this.nativeServerPtr = nativeInit();
			this.dispatchers = Collections.newSetFromMap(
        		new ConcurrentHashMap<Dispatcher, Boolean>());
		}

		@Override
		public void register(String prefix, Dispatcher dispatcher) throws VeyronException {
			// Hold a reference to the dispatcher so that native code doesn't have to do it.
			this.dispatchers.add(dispatcher);
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
			this.dispatchers.clear();
		}

		static {
			System.loadLibrary("ipcjni");
		}
	}

	// TODO(spetrovic): implement the client.
	private static class Client implements com.veyron2.ipc.Client {
		@Override
		public Call startCall(String name, String method, Object[] args) throws VeyronException {
			return null;
		}
		@Override
		public void close() {}
	}
}