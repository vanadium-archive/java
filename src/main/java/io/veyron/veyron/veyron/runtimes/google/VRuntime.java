package io.veyron.veyron.veyron.runtimes.google;

import io.veyron.veyron.veyron.runtimes.google.ipc.Client;
import io.veyron.veyron.veyron.runtimes.google.ipc.Server;
import io.veyron.veyron.veyron2.naming.Namespace;
import io.veyron.veyron.veyron2.OptionDefs;
import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.context.Context;
import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.security.Blessings;
import io.veyron.veyron.veyron2.security.CryptoUtil;
import io.veyron.veyron.veyron2.security.ECDSASigner;
import io.veyron.veyron.veyron2.security.Principal;
import io.veyron.veyron.veyron2.security.Security;
import io.veyron.veyron.veyron2.security.SecurityConstants;
import io.veyron.veyron.veyron2.security.Signer;

import java.security.KeyStore;
import java.security.interfaces.ECPublicKey;

/**
 * VRuntime is an implementation of VRuntime that calls to native Go code for most of its
 * functionalities.
 */
public class VRuntime implements io.veyron.veyron.veyron2.VRuntime {
	private static final String TAG = "Veyron runtime";
	private static VRuntime globalRuntime = null;

	private static native long nativeInit(Options opts) throws VeyronException;
	private static native long nativeNewRuntime(Options opts) throws VeyronException;

	/**
	 * Returns the initialized global instance of the runtime.
	 *
	 * @param  opts runtime options.
	 * @return      a pre-initialized runtime instance.
	 */
	public static synchronized VRuntime initRuntime(Options opts) throws VeyronException {
		if (VRuntime.globalRuntime == null) {
			// Use principal passed-in through options, if available.
			final Principal principal = (Principal)opts.get(OptionDefs.RUNTIME_PRINCIPAL);
			VRuntime.globalRuntime = new VRuntime(nativeInit(opts), principal);
		}
		return VRuntime.globalRuntime;
	}

	/**
	 * Returns the pre-initialized global runtime instance.  Returns {@code null} if init()
	 * hasn't already been invoked.
	 *
	 * @return a pre-initialized runtime instance.
	 */
	public static synchronized VRuntime defaultRuntime() {
		return VRuntime.globalRuntime;
	}

	/**
	 * Creates and initializes a new Runtime instance.
	 *
	 * @param  opts runtime options.
	 * @return      a new runtime instance.
	 */
	public static synchronized VRuntime newRuntime(Options opts) throws VeyronException {
		try {
			// Use principal passed-in through options, if available.
			final Principal principal = (Principal)opts.get(OptionDefs.RUNTIME_PRINCIPAL);
			return new VRuntime(nativeNewRuntime(opts), principal);
		} catch (VeyronException e) {
			throw new RuntimeException("Couldn't create Veyron Runtime: " + e.getMessage());
		}
	}

	private final long nativePtr;
	private Client client;
	private final Principal principal;

	private native Client nativeNewClient(long nativePtr, Options opts) throws VeyronException;
	private native Server nativeNewServer(long nativePtr, Options opts) throws VeyronException;
	private native Client nativeGetClient(long nativePtr) throws VeyronException;
	private native Context nativeNewContext(long nativePtr) throws VeyronException;
	private native Principal nativeGetPrincipal(long nativePtr) throws VeyronException;
	private native Namespace nativeGetNamespace(long nativePtr) throws VeyronException;
	private native void nativeFinalize(long nativePtr);

	private VRuntime(long nativePtr, Principal principal) {
		this.nativePtr = nativePtr;
		this.principal = principal;
	}
	@Override
	public io.veyron.veyron.veyron2.ipc.Client newClient() throws VeyronException {
		return newClient(null);
	}
	@Override
	public io.veyron.veyron.veyron2.ipc.Client newClient(Options opts) throws VeyronException {
		return nativeNewClient(this.nativePtr, opts);
	}
	@Override
	public io.veyron.veyron.veyron2.ipc.Server newServer() throws VeyronException {
		return newServer(null);
	}
	@Override
	public io.veyron.veyron.veyron2.ipc.Server newServer(Options opts) throws VeyronException {
		return nativeNewServer(this.nativePtr, opts);
	}
	@Override
	public synchronized io.veyron.veyron.veyron2.ipc.Client getClient() {
		if (this.client == null) {
			try {
				this.client = nativeGetClient(this.nativePtr);
			} catch (VeyronException e) {
				android.util.Log.e(TAG, "Coudln't get client: " + e.getMessage());
				return null;
			}
		}
		return this.client;
	}
	@Override
	public Context newContext() {
		try {
			return nativeNewContext(this.nativePtr);
		} catch (VeyronException e) {
			android.util.Log.e(TAG, "Couldn't get new context: " + e.getMessage());
			return null;
		}
	}
	@Override
	public Principal getPrincipal() {
		if (this.principal != null) {
			return this.principal;
		}
		try {
			return nativeGetPrincipal(this.nativePtr);
		} catch (VeyronException e) {
			android.util.Log.e(TAG, "Couldn't get principal: " + e.getMessage());
			return null;
		}
	}
	@Override
	public Namespace getNamespace() {
		try {
			return nativeGetNamespace(this.nativePtr);
		} catch (VeyronException e) {
			android.util.Log.e(TAG, "Couldn't get namespace: " + e.getMessage());
			return null;
		}
	}
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}