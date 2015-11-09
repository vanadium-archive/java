// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.v23;

import android.content.Context;

import io.v.v23.Options;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.security.BlessingStore;
import io.v.v23.security.Constants;
import io.v.v23.security.VPrincipal;
import io.v.v23.security.VSecurity;
import io.v.v23.security.VSigner;
import io.v.v23.verror.VException;

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
    private static native VContext nativeInitLogging(VContext ctx, Options opts) throws VException;

    private static void initLogging(Options opts) {
        try {
            context = nativeInitLogging(context, opts);
        } catch (VException e) {
            throw new RuntimeException("Couldn't initialize logging", e);
        }
    }

    /**
     * Initializes the Vanadium environment, returning the base context.  Calling this method
     * multiple times will always return the result of the first call to {@link #init init},
     * ignoring subsequently provided options.
     * <p>
     * A caller may pass the following option that specifies the runtime implementation to be used:
     * <p><ul>
     *     <li>{@link io.v.v23.OptionDefs#RUNTIME}</li>
     * </ul><p>
     * <p>
     * If this option isn't provided, the default runtime implementation is used.
     *
     * @param  androidCtx  Android application context
     * @param  opts        options for the default runtime
     * @return             base context
     */
    public static VContext init(Context androidCtx, Options opts) {
        if (initDone) return context;
        synchronized (V.class) {
            if (initDone) return context;
            if (androidCtx == null) {
                throw new RuntimeException("Android context must be non-null.");
            }
            if (opts == null) opts = new Options();
            initGlobal();
            initV(opts);
            initLogging(opts);
            // Set the VException component name to the Android context package name.
            context = VException.contextWithComponentName(context, androidCtx.getPackageName());
            try {
                // Initialize the principal.
                context = V.withPrincipal(context, createPrincipal(androidCtx));
            } catch (VException e) {
                throw new RuntimeException("Couldn't setup Vanadium principal", e);
            }
            initDone = true;
            return context;
        }
    }

    /**
     * Initializes the Vanadium environment without options.  See
     * {@link #init(Context,Options)} for more information.
     *
     * @return base context
     */
    public static VContext init(Context androidCtx) {
        return V.init(androidCtx, null);
    }

    private static VPrincipal createPrincipal(Context ctx) throws VException {
        // Check if the private key has already been generated for this package.
        // (NOTE: Android package names are unique.)
        KeyStore.PrivateKeyEntry keyEntry =
                KeyStoreUtil.getKeyStorePrivateKey(ctx.getPackageName());
        if (keyEntry == null) {
            // Generate a new private key.
            keyEntry = KeyStoreUtil.genKeyStorePrivateKey(ctx, ctx.getPackageName());
        }
        VSigner signer = VSecurity.newSigner(
                keyEntry.getPrivateKey(), (ECPublicKey)keyEntry.getCertificate().getPublicKey());
        VPrincipal principal =
                VSecurity.newPersistentPrincipal(signer, ctx.getFilesDir().getAbsolutePath());
        // Make sure we have at least one (i.e., self-signed) blessing in the store.
        BlessingStore store = principal.blessingStore();
        if (store.peerBlessings().isEmpty()) {
            Blessings self = principal.blessSelf(ctx.getPackageName());
            store.setDefaultBlessings(self);
            store.set(self, Constants.ALL_PRINCIPALS);
            VSecurity.addToRoots(principal, self);
        }
        return principal;
    }

    private V() {}
}
