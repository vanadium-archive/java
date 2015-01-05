package io.v.core.veyron2.android;

import android.content.Context;

import io.v.core.veyron2.OptionDefs;
import io.v.core.veyron2.Options;
import io.v.core.veyron2.VeyronException;
import io.v.core.veyron2.ipc.ListenSpec;
import io.v.core.veyron2.security.Blessings;
import io.v.core.veyron2.security.ECDSASigner;
import io.v.core.veyron2.security.Principal;
import io.v.core.veyron2.security.Security;
import io.v.core.veyron2.security.SecurityConstants;
import io.v.core.veyron2.security.Signer;

import java.security.KeyStore;
import java.security.interfaces.ECPublicKey;

/**
 * VRuntime represents the local android environment allowing clients and servers to communicate
 * with one another. The expected usage pattern of this class goes something like this:
 *
 *    ...
 *    VRuntime.init(getApplicationContext(), opts);
 *    ...
 *    final Server s = VRuntime.newServer();
 *    ...
 *    final Client c = VRuntime.getClient();
 *    ...
 *
 * This class is a convenience wrapper for android users.  It provides Android-related setup
 * and then delegates to the Java VRuntime methods.
 */
public class VRuntime extends io.v.core.veyron2.VRuntime {
	static {
		RedirectStderr.Start();
	}
	private static volatile boolean initialized = false;
	private static final ListenSpec DEFAULT_LISTEN_SPEC = new ListenSpec(
			new ListenSpec.Address[] { new ListenSpec.Address("tcp", ":0")},
			"/ns.dev.v.io:8101/proxy",
			true);

	/**
	 * Initializes the (singleton) runtime instance.  Calling this method multiple times will
	 * always return the result of the first call to {@code init()}, ignoring subsequently
	 * provided options.
	 *
	 * Invoking this method is optional; if not invoked, it will be auto-invoked with {@code null}
	 * options on a first call to any other method in this class.
	 *
	 * A caller may pass the following option that specifies the runtime implementation to be used:
	 *   {@code OptionDefs.RUNTIME}
	 *
	 * If this option isn't provided, the default runtime implementation is used; the rest of
	 * the options are passed to this runtime.  Currently, only the following options are
	 * recognized:
	 *   {@code OptionDefs.RUNTIME_PRINCIPAL}
	 *
	 * @param  ctx  Android application context.
	 * @param  opts runtime options.
	 */
	public static void init(Context ctx, Options opts) {
		if (initialized) return;
		synchronized (VRuntime.class) {
			if (initialized) return;
			initialized = true;
			if (opts == null) opts = new Options();
			if (opts.get(OptionDefs.RUNTIME) == null) {
				try {
		    		setupRuntimeOptions(ctx, opts);
				} catch (VeyronException e) {
		    		throw new RuntimeException(
		    			"Couldn't setup Google Veyron Runtime options: " + e.getMessage());
				}
			}
			if (opts.get(OptionDefs.DEFAULT_LISTEN_SPEC) == null) {
				opts.set(OptionDefs.DEFAULT_LISTEN_SPEC, DEFAULT_LISTEN_SPEC);
			}
			io.v.core.veyron2.VRuntime.init(opts);
		}
	}

	private static void setupRuntimeOptions(android.content.Context ctx, Options opts)
		throws VeyronException {
		if (!opts.has(OptionDefs.RUNTIME_PRINCIPAL) ||
			opts.get(OptionDefs.RUNTIME_PRINCIPAL) == null) {
			// Check if the private key has already been generated for this package.
			// (NOTE: Android package names are unique.)
			KeyStore.PrivateKeyEntry keyEntry =
				KeyStoreUtil.getKeyStorePrivateKey(ctx.getPackageName());
			if (keyEntry == null) {
				// Generate a new private key.
				keyEntry = KeyStoreUtil.genKeyStorePrivateKey(ctx, ctx.getPackageName());
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
}