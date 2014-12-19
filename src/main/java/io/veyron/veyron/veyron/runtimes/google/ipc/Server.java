package io.veyron.veyron.veyron.runtimes.google.ipc;

import io.veyron.veyron.veyron2.OptionDefs;
import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.ipc.Dispatcher;
import io.veyron.veyron.veyron2.ipc.ListenSpec;
import io.veyron.veyron.veyron2.ipc.ServiceObjectWithAuthorizer;

public class Server implements io.veyron.veyron.veyron2.ipc.Server {
	private static final ListenSpec DEFAULT_LISTEN_SPEC = new ListenSpec(
			new ListenSpec.Address[] { new ListenSpec.Address("tcp", ":0") },
			"/ns.dev.v.io:8101/proxy",
			false);

	private final long nativePtr;
	private final ListenSpec listenSpec;  // non-null.

	private native String[] nativeListen(long nativePtr, ListenSpec spec) throws VeyronException;
	private native void nativeServe(long nativePtr, String name, Dispatcher dispatcher)
		throws VeyronException;
	private native String[] nativeGetPublishedNames(long nativePtr) throws VeyronException;
	private native void nativeStop(long nativePtr) throws VeyronException;
	private native void nativeFinalize(long nativePtr);

	private Server(long nativePtr, Options opts) {
		this.nativePtr = nativePtr;
		this.listenSpec = opts.get(OptionDefs.DEFAULT_LISTEN_SPEC) != null
				? opts.get(OptionDefs.DEFAULT_LISTEN_SPEC, ListenSpec.class)
				: DEFAULT_LISTEN_SPEC;
	}
	// Implement io.veyron.veyron.veyron2.ipc.Server.
	@Override
	public String[] listen(ListenSpec spec) throws VeyronException {
		if (spec == null) {
			spec = this.listenSpec;
		}
		return nativeListen(this.nativePtr, spec);
	}
	@Override
	public void serve(String name, Object object) throws VeyronException {
		if (object instanceof Dispatcher) {
			nativeServe(this.nativePtr, name, (Dispatcher)object);
		} else {
			nativeServe(this.nativePtr, name, new DefaultDispatcher(object));
		}
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

	private static class DefaultDispatcher implements Dispatcher {
		private final Object obj;

		DefaultDispatcher(Object obj) {
			this.obj = obj;
		}
		@Override
		public ServiceObjectWithAuthorizer lookup(String suffix) throws VeyronException {
			return new ServiceObjectWithAuthorizer(this.obj, null);
		}
	}
}