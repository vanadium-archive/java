// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

import java.util.HashSet;
import java.util.Set;

/**
 * Lists all the remote principals ever blessed by the account manager.
 */
public class BlessedPrincipalsDisplayActivity extends PreferenceActivity {
    public static final String PUBLIC_KEY = "PUBLIC_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences principalsLog =
                getSharedPreferences(BlessActivity.LOG_PRINCIPALS, MODE_PRIVATE);

        // Create a preference screen with preferences for each principal blessed by the account
        // manager.
        PreferenceScreen prefScreen = getPreferenceManager().createPreferenceScreen(this);
        int numPrincipals = principalsLog.getInt(BlessActivity.NUM_PRINCIPALS_KEY, 0);

        for (int i = 0; i < numPrincipals; i++) {
            String principalPrefKey = BlessActivity.PRINCIPAL_NAMES_KEY + "_" + i;
            Set<String> onFailure = new HashSet<>();
            onFailure.add("Couldn't retrieve name(s)");
            Set<String> principalNames = principalsLog.getStringSet(principalPrefKey, onFailure);
            String title = "";
            for (String name: principalNames) {
                title += name + "\n";
            }
            Preference curPrincipalPref = new Preference(this);
            curPrincipalPref.setSummary(title);
            curPrincipalPref.setEnabled(true);

            Intent curPrincipalIntent = new Intent();
            curPrincipalIntent.setPackage("io.v.android.apps.account_manager");
            curPrincipalIntent.setClassName("io.v.android.apps.account_manager",
                    "io.v.android.apps.account_manager.BlessingEventsDisplayActivity");
            curPrincipalIntent.setAction(
                    "io.v.android.apps.account_manager.BLESSING_EVENTS_DISPLAY");
            curPrincipalIntent.putExtra(PUBLIC_KEY,
                    principalsLog.getString(BlessActivity.PRINCIPAL_KEY + "_" + i, ""));
            curPrincipalPref.setIntent(curPrincipalIntent);

            prefScreen.addPreference(curPrincipalPref);
        }
        setPreferenceScreen(prefScreen);
    }
}