package io.v.core.veyron2.android;

import io.v.core.veyron2.OptionDefs;
import io.v.core.veyron2.Options;
import io.v.core.veyron2.VeyronException;
import io.v.core.veyron2.context.VContext;
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
 * Class {@code V} represents the local android environment allowing clients and servers to
 * communicate with one another. The expected usage pattern of this class goes something like this:
 *
 *    ...
 *    final VContext ctx = V.init(getApplicationContext(), opts);
 *    ...
 *    final Server s = V.newServer(ctx);
 *    ...
 *    final Client c = V.getClient(ctx);
 *    ...
 *
 * This class is a convenience wrapper for android users.  It provides Android-related setup
 * and then delegates to the Java {@code V} methods.
 */
public class V extends io.v.core.veyron2.V {
	static {
		RedirectStderr.Start();
	}
	private static volatile VContext context = null;
	private static final ListenSpec DEFAULT_LISTEN_SPEC = new ListenSpec(
			new ListenSpec.Address[] { new ListenSpec.Address("tcp", ":0")},
			"/ns.dev.v.io:8101/proxy",
			true);

	/**
	 * Initializes the Veyron environment, returning the base context.  Calling this method multiple
	 * times will always return the result of the first call to {@code init()}, ignoring
	 * subsequently provided options.
	 *
	 * A caller may pass the following option that specifies the runtime implementation to be used:
	 *   {@code OptionDefs.RUNTIME}
	 *
	 * If this option isn't provided, the default runtime implementation is used.  The rest of
	 * the options are passed to this runtime.  Currently, only the following options are
	 * recognized:
	 *   CURRENTLY NO OPTIONS ARE SUPPORTED
	 *
	 * @param  ctx  Android application context
	 * @param  opts options for the default runtime
	 * @return      base context
	 */
	public static VContext init(android.content.Context ctx, Options opts) {
		if (context != null) return context;
		synchronized (V.class) {
			if (context != null) return context;
			if (opts == null) opts = new Options();
			if (opts.get(OptionDefs.DEFAULT_LISTEN_SPEC) == null) {
				opts.set(OptionDefs.DEFAULT_LISTEN_SPEC, DEFAULT_LISTEN_SPEC);
			}
			context = io.v.core.veyron2.V.init(opts);
			// Attach principal to the context.
			try {
				context = V.setPrincipal(context, createPrincipal(ctx, opts));
			} catch (VeyronException e) {
				throw new RuntimeException(
		    			"Couldn't setup Google Veyron Runtime options: " + e.getMessage());
			}
			return context;
		}
	}

	private static Principal createPrincipal(android.content.Context ctx, Options opts)
			throws VeyronException {
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
		final Principal principal = Security.newPrincipal(signer);
		final Blessings blessings = principal.blessSelf(ctx.getPackageName());
		principal.blessingStore().setDefaultBlessings(blessings);
		principal.blessingStore().set(blessings, SecurityConstants.ALL_PRINCIPALS);
		principal.addToRoots(blessings);
		return principal;
	}
}