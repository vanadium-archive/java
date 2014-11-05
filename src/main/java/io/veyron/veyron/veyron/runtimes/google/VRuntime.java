package io.veyron.veyron.veyron.runtimes.google;

import io.veyron.veyron.veyron.runtimes.google.android.RedirectStderr;
import io.veyron.veyron.veyron.runtimes.google.ipc.Client;
import io.veyron.veyron.veyron.runtimes.google.ipc.Server;
import io.veyron.veyron.veyron.runtimes.google.naming.Namespace;
import io.veyron.veyron.veyron2.OptionDefs;
import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.context.Context;
import io.veyron.veyron.veyron2.ipc.VeyronException;
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
	 * @param  ctx  android context.
	 * @param  opts runtime options.
	 * @return      a pre-initialized runtime instance.
	 */
	public static synchronized VRuntime initRuntime(android.content.Context ctx, Options opts) {
		if (VRuntime.globalRuntime == null) {
			if (opts == null) {
				opts = new Options();
			}
			try {
				setupRuntimeOptions(ctx, opts);
				final Principal principal = (Principal)opts.get(OptionDefs.RUNTIME_PRINCIPAL);
				VRuntime.globalRuntime = new VRuntime(nativeInit(opts), principal);
			} catch (VeyronException e) {
				throw new RuntimeException(
					"Couldn't initialize global Veyron Runtime instance: " + e.getMessage());
			}
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
	 * @param  ctx  android context.
	 * @param  opts runtime options.
	 * @return      a new runtime instance.
	 */
	public static synchronized VRuntime newRuntime(android.content.Context ctx, Options opts) {
		if (opts == null) {
			opts = new Options();
		}
		try {
			setupRuntimeOptions(ctx, opts);
			final Principal principal = (Principal)opts.get(OptionDefs.RUNTIME_PRINCIPAL);
			return new VRuntime(nativeNewRuntime(opts), principal);
		} catch (VeyronException e) {
			throw new RuntimeException("Couldn't create Veyron Runtime: " + e.getMessage());
		}
	}

	private static void setupRuntimeOptions(android.content.Context ctx, Options opts)
		throws VeyronException {
		if (!opts.has(OptionDefs.RUNTIME_PRINCIPAL) ||
			opts.get(OptionDefs.RUNTIME_PRINCIPAL) == null) {
			// Check if the private key has already been generated for this package.
			// (NOTE: Android package names are unique.)
			KeyStore.PrivateKeyEntry keyEntry =
				CryptoUtil.getKeyStorePrivateKey(ctx.getPackageName());
			if (keyEntry == null) {
				// Generate a new private key.
				keyEntry = CryptoUtil.genKeyStorePrivateKey(ctx, ctx.getPackageName());
			}
			final Signer signer = new ECDSASigner(
				keyEntry.getPrivateKey(), (ECPublicKey)keyEntry.getCertificate().getPublicKey());
			final Principal principal = createPrincipal(ctx, signer);
			opts.set(OptionDefs.RUNTIME_PRINCIPAL, principal);
		}
	}

	private static Principal createPrincipal(android.content.Context ctx, Signer signer)
		throws VeyronException {
		final Principal principal = Security.newPrincipal(signer);
		final Blessings blessings = principal.blessSelf(ctx.getPackageName());
		principal.blessingStore().setDefaultBlessings(blessings);
		principal.blessingStore().set(blessings, SecurityConstants.ALL_PRINCIPALS);
		principal.addToRoots(blessings);
		return principal;
	}

	static {
		System.loadLibrary("jniwrapper");
		System.loadLibrary("veyronjni");
		RedirectStderr.Start();
	}

	private final long nativePtr;
	private Client client;
	private final Principal principal;  // non-null

	private native Client nativeNewClient(long nativePtr, Options opts) throws VeyronException;
	private native Server nativeNewServer(long nativePtr, Options opts) throws VeyronException;
	private native Client nativeGetClient(long nativePtr) throws VeyronException;
	private native Context nativeNewContext(long nativePtr) throws VeyronException;
	private native long nativeGetNamespace(long nativePtr);
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
		return this.principal;
	}
	@Override
	public io.veyron.veyron.veyron2.naming.Namespace getNamespace() {
		return new Namespace(nativeGetNamespace(this.nativePtr));
	}
	@Override
	protected void finalize() {
		nativeFinalize(this.nativePtr);
	}
}