package io.v.core.veyron.runtimes.google.ipc;

import io.v.core.veyron.runtimes.google.InputChannel;
import io.v.core.veyron2.ipc.Dispatcher;
import io.v.core.veyron2.ipc.ListenSpec;
import io.v.core.veyron2.ipc.NetworkChange;
import io.v.core.veyron2.ipc.ServerStatus;
import io.v.core.veyron2.ipc.ServiceObjectWithAuthorizer;
import io.v.core.veyron2.verror.VException;

public class Server implements io.v.core.veyron2.ipc.Server {
	private final long nativePtr;
	private final ListenSpec listenSpec;

	private native String[] nativeListen(long nativePtr, ListenSpec spec) throws VException;
	private native void nativeServe(long nativePtr, String name, Dispatcher dispatcher)
		throws VException;
	private native void nativeAddName(long nativePtr, String name) throws VException;
	private native void nativeRemoveName(long nativePtr, String name);
	private native ServerStatus nativeGetStatus(long nativePtr) throws VException;
	private native InputChannel<NetworkChange> nativeWatchNetwork(long nativePtr) throws VException;
	private native void nativeUnwatchNetwork(long nativePtr, InputChannel<NetworkChange> channel)
			throws VException;
	private native void nativeStop(long nativePtr) throws VException;
	private native void nativeFinalize(long nativePtr);

	private Server(long nativePtr, ListenSpec spec) {
		this.nativePtr = nativePtr;
		this.listenSpec = spec;
	}
	// Implement io.v.core.veyron2.ipc.Server.
	@Override
	public String[] listen(ListenSpec spec) throws VException {
		if (spec == null) {
			spec = this.listenSpec;
		}
		if (spec == null) {
			spec = ListenSpec.DEFAULT;
		}
		return nativeListen(this.nativePtr, spec);
	}
	@Override
	public void serve(String name, Object object) throws VException {
		if (object instanceof Dispatcher) {
			nativeServe(this.nativePtr, name, (Dispatcher)object);
		} else {
			nativeServe(this.nativePtr, name, new DefaultDispatcher(object));
		}
	}
	@Override
	public void addName(String name) throws VException {
		nativeAddName(this.nativePtr, name);
	}
	@Override
	public void removeName(String name) {
		nativeRemoveName(this.nativePtr, name);
	}
	@Override
	public ServerStatus getStatus() {
		try {
			return nativeGetStatus(this.nativePtr);
		} catch (VException e) {
			throw new RuntimeException("Couldn't get status: " + e.getMessage());
		}
	}
	@Override
	public io.v.core.veyron2.InputChannel<NetworkChange> watchNetwork() {
		try {
			return nativeWatchNetwork(this.nativePtr);
		} catch (VException e) {
			throw new RuntimeException("Couldn't watch network: " + e.getMessage());
		}
	}
	@Override
	public void unwatchNetwork(io.v.core.veyron2.InputChannel<NetworkChange> channel) {
		if (!(channel instanceof InputChannel)) {  // also handles channel == null
			return;
		}
		try {
			nativeUnwatchNetwork(this.nativePtr, (InputChannel<NetworkChange>) channel);
		} catch (VException e) {
			throw new RuntimeException("Couldn't unwatch network: " + e.getMessage());
		}
	}
	@Override
	public void stop() throws VException {
		nativeStop(this.nativePtr);
	}
	// Implement java.lang.Object.
	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if (other == null) return false;
		if (this.getClass() != other.getClass()) return false;
		return this.nativePtr == ((Server) other).nativePtr;
	}
	@Override
	public int hashCode() {
		return Long.valueOf(this.nativePtr).hashCode();
	}
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}

	private static class DefaultDispatcher implements Dispatcher {
		private final Object obj;

		DefaultDispatcher(Object obj) {
			this.obj = obj;
		}
		@Override
		public ServiceObjectWithAuthorizer lookup(String suffix) throws VException {
			return new ServiceObjectWithAuthorizer(this.obj, null);
		}
	}
}