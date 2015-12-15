// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.libs.security;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.security.interfaces.ECPublicKey;
import java.util.concurrent.ConcurrentHashMap;

import io.v.android.impl.google.services.blessing.BlessingActivity;
import io.v.android.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.security.Constants;
import io.v.v23.security.VPrincipal;
import io.v.v23.security.VSecurity;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * Manages {@link Blessings} for a given Android application, persisting them in its
 * shared preferences.
 * <p>
 * This class is thread-safe.
 */
public class BlessingsManager {
    private static String TAG = "BlessingsManager";

    private static ConcurrentHashMap<String, ListenableFuture<Blessings>> mintsInProgress =
            new ConcurrentHashMap<>();

    /**
     * A shortcut for {@link #mintBlessings(Context, String, String, boolean)}} with empty
     * Google account, causing the user to be prompted to pick one of the installed Google
     * accounts (if there is more than one installed).
     */
    public static ListenableFuture<Blessings> mintBlessings(
            Context context, final String key, boolean setAsDefault) {
        return mintBlessings(context, key, "", setAsDefault);
    }

    /**
     * Mints a new set of {@link Blessings} that are persisted in {@link SharedPreferences} under
     * the provided key.
     * <p>
     * If {@code googleAccount} is non-{@code null} and non-empty, mints the blessings using
     * that account;  otherwise, prompts the user to pick one of the installed Google accounts
     * (if there is more than one installed).
     *
     * @param context       android {@link Context}
     * @param key           a key in {@link SharedPreferences} under which the newly minted
     *                      blessings are persisted
     * @param googleAccount a Google account to use to mint the blessings; if {@code null} or
     *                      empty, user will be prompted to pick one of the installed Google
     *                      accounts, if there is more than one installed
     * @param setAsDefault  if true, the returned {@link Blessings} will be set as default
     *                      blessings for the app
     * @return              a new {@link ListenableFuture} whose result are the newly minted
     *                      {@link Blessings}
     */
    public static synchronized ListenableFuture<Blessings> mintBlessings(
            final Context context, String key, String googleAccount, boolean setAsDefault) {
        final String prefKey = key;
        {
            ListenableFuture<Blessings> future = mintsInProgress.get(prefKey);
            if (future != null) {
                return setAsDefault ? wrapWithSetAsDefault(context, future) : future;
            }
        }
        final SettableFuture<Blessings> future = SettableFuture.create();
        mintsInProgress.put(prefKey, future);
        final String errorPrefKey = BlessingActivity.PREF_ERROR_KEY_PREFIX + key;
        final SharedPreferences.OnSharedPreferenceChangeListener listener =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (prefKey.equals(errorPrefKey)) {  // blessing error
                    future.setException(new VException(prefs.getString(
                            errorPrefKey, "Blessing error")));
                    return;
                }
                if (prefKey.equals(key)) {
                    try {
                        Blessings blessings = readBlessings(prefs, prefKey);
                        if (blessings == null) {
                            future.setException(new VException("Got null Blessings"));
                        } else {
                            future.set(blessings);
                        }
                    } catch (VException e) {
                        future.setException(e);
                    }
                }
            }
        };
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.registerOnSharedPreferenceChangeListener(listener);
        VContext vCtx = V.init(context);
        ECPublicKey pubKey = V.getPrincipal(vCtx).publicKey();
        Intent intent = new Intent(context, BlessingActivity.class);
        intent.putExtra(BlessingActivity.EXTRA_PUBLIC_KEY, pubKey);
        intent.putExtra(BlessingActivity.EXTRA_PREF_KEY, prefKey);
        if (googleAccount != null && !googleAccount.isEmpty()) {
            intent.putExtra(BlessingActivity.EXTRA_GOOGLE_ACCOUNT, googleAccount);
        }
        context.startActivity(intent);

        ListenableFuture<Blessings> ret = Futures.withFallback(
                Futures.transform(future, new AsyncFunction<Blessings, Blessings>() {
                    @Override
                    public ListenableFuture<Blessings> apply(Blessings blessings) throws Exception {
                        prefs.unregisterOnSharedPreferenceChangeListener(listener);
                        mintsInProgress.remove(prefKey);
                        return Futures.immediateFuture(blessings);
                    }
                }), new FutureFallback<Blessings>() {
                    @Override
                    public ListenableFuture<Blessings> create(Throwable t) throws Exception {
                        prefs.unregisterOnSharedPreferenceChangeListener(listener);
                        mintsInProgress.remove(prefKey);
                        return Futures.immediateFailedFuture(t);
                    }
                });
        return setAsDefault ? wrapWithSetAsDefault(context, ret) : ret;
    }

    private static ListenableFuture<Blessings> wrapWithSetAsDefault(
            final Context context, ListenableFuture<Blessings> future) {
        return Futures.transform(future, new AsyncFunction<Blessings, Blessings>() {
            @Override
            public ListenableFuture<Blessings> apply(Blessings blessings) throws Exception {
                try {
                    // Update local state with the new blessings.
                    VContext vCtx = V.init(context);
                    VPrincipal p = V.getPrincipal(vCtx);
                    p.blessingStore().setDefaultBlessings(blessings);
                    p.blessingStore().set(blessings, Constants.ALL_PRINCIPALS);
                    VSecurity.addToRoots(p, blessings);
                    return Futures.immediateFuture(blessings);
                } catch (VException e) {
                    return Futures.immediateFailedFuture(e);
                }
            }
        });
    }

    /**
     * Returns a new {@link ListenableFuture} whose result are the {@link Blessings} found in
     * {@link SharedPreferences} under the given key.
     * <p>
     * If no {@link Blessings} are found, mints a new set of {@link Blessings} and stores them
     * in {@link SharedPreferences} under the provided key.
     *
     * @param  context     android {@link Context}
     * @param  key         a key under which the blessings are stored
     * @param setAsDefault if true, the returned {@link Blessings} will be set as default
     *                     blessings for the app
     * @return             a new {@link ListenableFuture} whose result are the blessings
     *                     persisted under the given key
     */
    public static ListenableFuture<Blessings> getBlessings(
            Context context, String key, boolean setAsDefault) {
        try {
            Blessings blessings =
                    readBlessings(PreferenceManager.getDefaultSharedPreferences(context), key);
            if (blessings != null) {
                ListenableFuture<Blessings> ret = Futures.immediateFuture(blessings);
                return setAsDefault ? wrapWithSetAsDefault(context, ret) : ret;
            }
        } catch (VException e) {
            Log.e(TAG, "Malformed blessings in SharedPreferences. Minting new blessings: " +
                    e.getMessage());
        }
        return mintBlessings(context, key, setAsDefault);
    }

    private static Blessings readBlessings(SharedPreferences prefs, String prefKey)
            throws VException {
        String blessingsVom = prefs.getString(prefKey, "");
        if (blessingsVom == null || blessingsVom.isEmpty()) {
            return null;
        }
        return (Blessings) VomUtil.decodeFromString(blessingsVom, Blessings.class);
    }

    private BlessingsManager() {}
}