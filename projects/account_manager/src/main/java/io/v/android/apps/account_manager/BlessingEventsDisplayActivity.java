// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import java.util.Map;
import java.util.Set;

import io.v.v23.android.V;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.security.Caveat;
import io.v.v23.security.VPrincipal;
import io.v.v23.vom.VomUtil;

/**
 * Lists all the blessings given to a particular app.
 */
public class BlessingEventsDisplayActivity extends PreferenceActivity {
    private static final String TAG = "BlessingEventsDisplay"; // bounded by 23 character limit
    VContext mBaseContext = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBaseContext = V.init(this);

        SharedPreferences blessingsLog =
                getSharedPreferences(BlessingActivity.LOG_BLESSINGS, MODE_PRIVATE);
        String callingPkgName =
                getIntent().getExtras().getString(BlessedPackagesDisplayActivity.PACKAGE_NAME_KEY);

        PreferenceScreen prefScreen = getPreferenceManager().createPreferenceScreen(this);
        int n = blessingsLog.getInt(callingPkgName, 0);

        for (int i = Math.max(0, n - BlessingActivity.MAX_BLESSINGS_FOR_PACKAGE); i < n; i++) {
            String key = callingPkgName + "_" + i;

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
                    android.util.Log.e(TAG, "BlessingEvent not found or improperly serialized.");
                }
            }
        }
        setPreferenceScreen(prefScreen);
    }
}