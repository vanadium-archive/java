// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.libs.security;

import com.google.common.collect.ImmutableList;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import io.v.v23.android.V;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.security.VCertificate;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

import java.security.interfaces.ECPublicKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages blessings for a given Android application, persisting them in its
 * shared preferences.
 */
public class BlessingsManager {
    private static final String TAG = "BlessingsManager";

    private static final String BLESSING_PKG = "io.v.android.apps.account_manager";
    private static final String BLESSING_ACTIVITY = "BlessingActivity";
    private static final String BLESSEE_PUBKEY_KEY = "BLESSEE_PUBKEY";
    private static final String BLESSINGS_PREF_KEY = "vanadium_blessings";

    private static final String ERROR = "ERROR";
    private static final String REPLY = "REPLY";

    /**
     * Creates the intent for obtaining blessings from the Vanadium Account Manager.
     *
     * @param ctx android context
     * @return    intent used to obtaining blessings from the Vanadium Account Manager
     */
    public static Intent createIntent(Context ctx) {
        VContext vCtx = V.init(ctx);
        ECPublicKey key = V.getPrincipal(vCtx).publicKey();
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                BLESSING_PKG, BLESSING_PKG + "." + BLESSING_ACTIVITY));
        intent.putExtra(BLESSEE_PUBKEY_KEY, key);
        return intent;
    }

    /**
     * Processes the reply from the Vanadium Account Manager, returning the blessings stored
     * within.
     *
     * @param  resultCode      result code of the reply
     * @param  data            reply data
     * @return                 the blessings stored in the reply
     * @throws VException      if the blessings couldn't be extracted from the reply
     */
    public static Blessings processReply(int resultCode, Intent data) throws VException {
        if (data == null) {
            throw new VException("NULL blessing response");
        }
        if (resultCode != Activity.RESULT_OK) {
            throw new VException("Error getting blessing: " + data.getStringExtra(ERROR));
        }
        Blessings blessings = (Blessings) data.getSerializableExtra(REPLY);
        if (blessings == null) {
            throw new VException("Got null blessings.");
        }
        if (blessings.getCertificateChains() == null ||
                blessings.getCertificateChains().size() <= 0) {
            throw new VException("Got empty blessings.");
        }
        return blessings;
    }

    private final Map<String, Blessings> mBlessings;
    private final SharedPreferences mPrefs;

    /**
     * Constructs the blessings manager that persists the blessings in the provided shared
     * preferences.
     *
     * @param prefs shared preferences used for persisting blessings
     */
    public BlessingsManager(SharedPreferences prefs) {
        mBlessings = new HashMap<String, Blessings>();
        mPrefs = prefs;
        loadPrefs();
    }

    /**
     * Returns the names of all the blessings stored in the manager.
     *
     * @return names of all the blessings stored in the manager
     */
    public synchronized Set<String> getNames() {
        return mBlessings.keySet();
    }

    /**
     * Adds the provided blessings to the manager.
     *
     * @param blessings the blessings to be added to the manager
     */
    public synchronized void add(Blessings blessings) {
        Blessings[] splitBlessings = splitBlessings(blessings);
        for (Blessings blessing : splitBlessings) {
            String name = getBlessingName(blessing);
            if (mBlessings.containsKey(name) && mBlessings.get(name).equals(blessing)) {
                // Nothing to update.
                return;
            }
            mBlessings.put(name, blessing);
        }
        storePrefs();
    }

    /**
     * Removes the blessing with the given name from the manager.
     *
     * @param name a name of the blessing to be removed.
     */
    public synchronized void remove(String name) {
        mBlessings.remove(name);
        storePrefs();
    }

    /**
     * Returns the blessing with the given name.
     *
     * @return blessing with the given name
     */
    public synchronized Blessings get(String name) {
        return mBlessings.get(name);
    }

    private void loadPrefs() {
        Set<String> blessings = mPrefs.getStringSet(BLESSINGS_PREF_KEY, null);
        if (blessings == null) return;
        for (String blessingStr : blessings) {
            try {
                Blessings blessing = vomDecodeBlessing(blessingStr);
                String name = getBlessingName(blessing);
                mBlessings.put(name, blessing);
            } catch (VException e) {
                android.util.Log.e(TAG, "Couldn't decode blessing, skipping: " + blessingStr);
            }
        }
    }

    private void storePrefs() {
        Set<String> blessings = new HashSet<String>();
        for (Blessings blessing : mBlessings.values()) {
            try {
                blessings.add(vomEncodeBlessing(blessing));
            } catch (VException e) {
                android.util.Log.e(TAG, "Couldn't encode blessing: " + blessing);
            }
        }
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putStringSet(BLESSINGS_PREF_KEY, blessings);
        editor.commit();
    }

    private static Blessings[] splitBlessings(Blessings blessings) {
        if (blessings == null || blessings.getCertificateChains() == null) {
            return new Blessings[0];
        }
        List<List<VCertificate>> chains = blessings.getCertificateChains();
        if (chains.size() == 1) {
            return new Blessings[] { blessings };
        }
        Blessings[] ret = new Blessings[chains.size()];
        for (int i = 0; i < chains.size(); ++i) {
            ret[i] = new Blessings(ImmutableList.<List<VCertificate>>of(chains.get(i)));
        }
        return ret;
    }

    private static String getBlessingName(Blessings blessing) {
        if (blessing == null) {
            return "";
        }
        List<VCertificate> chain = blessing.getCertificateChains().get(0);
        String ret = "";
        for (int i = 0; i < chain.size(); ++i) {
            ret += chain.get(i).getExtension();
            ret += "/";
        }
        return ret;
    }

    private static String vomEncodeBlessing(Blessings blessings) throws VException {
        return VomUtil.encodeToString(blessings, Blessings.class);
    }

    private static Blessings vomDecodeBlessing(String encoded) throws VException {
        return (Blessings) VomUtil.decodeFromString(encoded, Blessings.class);
    }
}