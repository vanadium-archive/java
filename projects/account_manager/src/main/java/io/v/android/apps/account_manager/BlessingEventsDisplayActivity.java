// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.widget.Toast;

import java.util.Map;
import java.util.Set;

import io.v.v23.android.V;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.security.Caveat;
import io.v.v23.security.VPrincipal;
import io.v.v23.vom.VomUtil;

/**
 * Lists all the blessings given to a particular principal.
 */
public class BlessingEventsDisplayActivity extends PreferenceActivity {
    private static final String TAG = "BlessingEventsDisplay"; // 23 character limit
    VContext mBaseContext = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBaseContext = V.init(this);

        SharedPreferences blessingsLog =
                getSharedPreferences(BlessActivity.LOG_BLESSINGS, MODE_PRIVATE);
        String principalPublicKey =
                getIntent().getStringExtra(BlessedPrincipalsDisplayActivity.PUBLIC_KEY);

        PreferenceScreen prefScreen = getPreferenceManager().createPreferenceScreen(this);
        int n = blessingsLog.getInt(principalPublicKey, 0);

        for (int i = Math.max(0, n - BlessActivity.MAX_BLESSINGS_FOR_REMOTE_PRINCIPAL); i < n; i++)
        {
            String key = principalPublicKey + "_" + i;

            if (blessingsLog.contains(key)) {
                String encoded = blessingsLog.getString(key, "");
                try {
                    // Decode the blessing event to recover the set of blessing names that were
                    // given out.
                    BlessingEvent event = BlessingEvent.decode(encoded);
                    String blessingsVom = event.getBlessingsVom();
                    Blessings blessings =
                            (Blessings) VomUtil.decodeFromString(blessingsVom, Blessings.class);
                    VPrincipal principal = V.getPrincipal(mBaseContext);
                    principal.addToRoots(blessings); /* NOTE(sjayanti): Should be done elsewhere */
                    Map<String, Caveat[]> blessingsMap = principal.blessingsInfo(blessings);
                    Set<String> blessingNames = blessingsMap.keySet();

                    // Create a new preference for each blessing.
                    for (String name: blessingNames) {
                        Preference currentPref = new Preference(this);
                        currentPref.setSummary(name + "/" + event.getNameExtension());
                        currentPref.setEnabled(true);
                        prefScreen.addPreference(currentPref);
                    }
                } catch (Exception e) {
                    String msg = "BlessingEvent not found or improperly serialized.";
                    android.util.Log.e(TAG, msg);
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                }
            }
        }
        setPreferenceScreen(prefScreen);
    }
}