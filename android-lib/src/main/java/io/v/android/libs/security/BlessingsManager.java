// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.libs.security;

import android.content.Context;
import android.content.SharedPreferences;

import io.v.v23.security.Blessings;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * Manages {@link Blessings} for a given Android application, persisting them in its
 * shared preferences.
 * <p>
 * This class is thread-safe.
 */
public class BlessingsManager {
    private static final String PREF_NAME = "VanadiumBlessings";
    private static final String PREF_KEY = "VanadiumBlessings";

    /**
     * Persists the given {@link Blessings} in the provided {@link Context}'s shared preferences.
     *
     * @param  ctx        {@link Context} where the blessings will be stored
     * @param  blessings  {@link Blessings} to store in the provided {@link Context}
     * @throws VException if the {@link Blessings} couldn't be stored
     */
    public static synchronized void addBlessings(Context ctx, Blessings blessings)
            throws VException {
        String blessingsVom = VomUtil.encodeToString(blessings, Blessings.class);
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_NAME, ctx.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_KEY, blessingsVom);
        editor.commit();
    }

    /**
     * Returns the {@link Blessings} persisted in the provided {@link Context}
     * (via {@link #addBlessings}), or {@code null} if no {@link Blessings} were persisted.
     *
     * @param  ctx        {@link Context} from which the blessings are retrieved
     * @return            {@link Blessings} stored in the provided {@link Context}, or {@code null}
     *                    if no {@link Blessings} are stored in the {@link Context}
     * @throws VException if the {@link Blessings} couldn't be retrieved
     */
    public static synchronized Blessings getBlessings(Context ctx) throws VException {
        SharedPreferences prefs = ctx.getSharedPreferences(PREF_NAME, ctx.MODE_PRIVATE);
        String blessingsVom = prefs.getString(PREF_KEY, "");
        if (blessingsVom == null || blessingsVom.isEmpty()) {
            return null;
        }
        return (Blessings) VomUtil.decodeFromString(blessingsVom, Blessings.class);
    }

    private BlessingsManager() {}
}