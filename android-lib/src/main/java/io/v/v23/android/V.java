// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.v23.android;

import io.v.v23.Options;
import io.v.v23.verror.VException;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.security.Principal;
import io.v.v23.security.Security;
import io.v.v23.security.Constants;
import io.v.v23.security.Signer;

import java.security.KeyStore;
import java.security.interfaces.ECPublicKey;

/**
 * The local android environment allowing clients and servers to communicate with one another.
 * The expected usage pattern of this class goes something like this:
 * <p><blockquote><pre>
 *    final VContext ctx = V.init(getApplicationContext(), opts);
 *    ...
 *    final Server s = V.newServer(ctx);
 *    ...
 *    final Client c = V.getClient(ctx);
 *    ...
 * </pre></blockquote><p>
 * This class is a convenience wrapper for android users.  It provides Android-related setup
 * and then delegates to the Java {@link io.v.v23.V} methods.
 */
public class V extends io.v.v23.V {
    private static volatile VContext context = null;

    /**
     * Initializes the Vanadium environment, returning the base context.  Calling this method
     * multiple times will always return the result of the first call to {@link #init init},
     * ignoring subsequently provided options.
     * <p>
     * A caller may pass the following option that specifies the runtime implementation to be used:
     * <p><ul>
     *     <li>{@link OptionDefs.RUNTIME}</li>
     * </ul><p>
     * <p>
     * If this option isn't provided, the default runtime implementation is used.
     *
     * @param  androidCtx  Android application context
     * @param  opts        options for the default runtime
     * @return             base context
     */
    public static VContext init(android.content.Context androidCtx, Options opts) {
        if (context != null) return context;
        synchronized (V.class) {
            if (context != null) return context;
            if (androidCtx == null) {
                throw new RuntimeException("Android context must be non-null.");
            }
            context = io.v.v23.V.init(opts);
            RedirectStderr.Start();
            // Attach principal and listen spec to the context.
            try {
                context = V.setPrincipal(context, createPrincipal(androidCtx));
            } catch (VException e) {
                throw new RuntimeException("Couldn't setup Vanadium Runtime options", e);
            }
            // Set the VException component name to the Android context package name.
            context = VException.contextWithComponentName(context, androidCtx.getPackageName());
            return context;
        }
    }

    /**
     * Initializes the Veyron environment without options.  See
     * {@link #init(android.content.Context,Options)} for more information.
     *
     * @return base context
     */
    public static VContext init(android.content.Context ctx) {
        return V.init(ctx, null);
    }

    private static Principal createPrincipal(android.content.Context ctx) throws VException {
        // Check if the private key has already been generated for this package.
        // (NOTE: Android package names are unique.)
        KeyStore.PrivateKeyEntry keyEntry =
                KeyStoreUtil.getKeyStorePrivateKey(ctx.getPackageName());
        if (keyEntry == null) {
            // Generate a new private key.
            keyEntry = KeyStoreUtil.genKeyStorePrivateKey(ctx, ctx.getPackageName());
        }
        final Signer signer = Security.newSigner(
                keyEntry.getPrivateKey(), (ECPublicKey)keyEntry.getCertificate().getPublicKey());
        final Principal principal = Security.newPrincipal(signer);
        final Blessings blessings = principal.blessSelf(ctx.getPackageName());
        principal.blessingStore().setDefaultBlessings(blessings);
        principal.blessingStore().set(blessings, Constants.ALL_PRINCIPALS);
        principal.addToRoots(blessings);
        return principal;
    }

    private V() {}
}
