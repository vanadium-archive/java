package io.veyron.veyron.veyron2.android;

import android.content.Context;

import io.veyron.veyron.veyron2.OptionDefs;
import io.veyron.veyron.veyron2.Options;
import io.veyron.veyron.veyron2.VRuntime;
import io.veyron.veyron.veyron2.VeyronException;
import io.veyron.veyron.veyron2.security.Blessings;
import io.veyron.veyron.veyron2.security.ECDSASigner;
import io.veyron.veyron.veyron2.security.Principal;
import io.veyron.veyron.veyron2.security.Security;
import io.veyron.veyron.veyron2.security.SecurityConstants;
import io.veyron.veyron.veyron2.security.Signer;

import java.security.KeyStore;
import java.security.interfaces.ECPublicKey;

/**
 * RuntimeFactory creates new runtimes for Android.  It represents an entry point into the Veyron
 * codebase for Android apps.  The expected usage pattern of this class goes something like this:
 *
 *    RuntimeFactory.initRuntime(...);
 *    ...
 *    final Runtime r = RuntimeFactory.defaultRuntime();
 *    final Server s = r.newServer();
 *    ...
 *    final Client c = r.getClient();
 *    ...
 *
 * This class is a convenience wrapper for android users.  It provides Android-related setup
 * and then invokes the default RuntimeFactory.
 */
public class RuntimeFactory {
	static {
		// Forces the static initialization block of the base RuntimeFactory to be run.
		new io.veyron.veyron.veyron2.RuntimeFactory();
		RedirectStderr.Start();
	}

	/**
	 * Initializes the global instance of the runtime.  Calling this method multiple times
	 * will always return the result of the first call to {@code init()} (ignoring subsequently
	 * provided options). All Veyron apps should call {@code init()} as the first thing in their
	 * execution flow.
	 *
	 * @param ctx   android context.
	 * @param opts  runtime options.
	 * @return      a pre-initialized runtime instance.
	 */
	public static synchronized VRuntime initRuntime(Context ctx, Options opts) {
		if (opts == null) {
			opts = new Options();
		}
		setupRuntimeOptions(ctx, opts);
		return io.veyron.veyron.veyron2.RuntimeFactory.initRuntime(opts);
	}

	/**
	 * Returns the global, pre-initialized instance of a runtime, i.e., the runtime instance
	 * returned by the first call to {@code init()}.  This method requires that {@code init()}
	 * has already been invoked.
	 *
	 * @return default runtime instance.
	 */
	public static synchronized VRuntime defaultRuntime() throws VeyronException {
		return io.veyron.veyron.veyron2.RuntimeFactory.defaultRuntime();
	}

	/**
	 * Creates and initializes a new runtime instance.  This method should be used in unit tests
	 * and any situation where a single global runtime instance is inappropriate.
	 *
	 * @param ctx   android context.
	 * @param opts  runtime options.
	 * @return      a new runtime instance.
	 */
	public static VRuntime newRuntime(Context ctx, Options opts) throws VeyronException {
		if (opts == null) {
			opts = new Options();
		}
		setupRuntimeOptions(ctx, opts);
		return io.veyron.veyron.veyron2.RuntimeFactory.newRuntime(opts);
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