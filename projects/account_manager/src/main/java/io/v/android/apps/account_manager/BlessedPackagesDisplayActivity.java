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

/**
 * Lists all the packages ever blessed by the account manager.
 */
public class BlessedPackagesDisplayActivity extends PreferenceActivity {
    public static final String PACKAGE_NAME_KEY = "PACKAGE_NAME_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences pkgLog =
                getSharedPreferences(BlessingActivity.LOG_PACKAGES, MODE_PRIVATE);

        // Create a preference screen with preferences for each package blessed by the account
        // manager.
        PreferenceScreen prefScreen = getPreferenceManager().createPreferenceScreen(this);
        int numPkgs = pkgLog.getInt(BlessingActivity.NUM_PACKAGES_KEY, 0);

        for (int i = 0; i < numPkgs; i++) {
            String pkgNamePrefKey = BlessingActivity.PACKAGE_KEY + "_" + i;
            String pkgName = pkgLog.getString(pkgNamePrefKey, "Couldn't retrieve name");

            Preference currentPkgPref = new Preference(this);
            currentPkgPref.setTitle(pkgName);
            currentPkgPref.setEnabled(true);

            Intent currentPkgIntent = new Intent();
            currentPkgIntent.setPackage("io.v.android.apps.account_manager");
            currentPkgIntent.setClassName("io.v.android.apps.account_manager",
                    "io.v.android.apps.account_manager.BlessingEventsDisplayActivity");
            currentPkgIntent.setAction("io.v.android.apps.account_manager.BLESSING_EVENTS_DISPLAY");
            currentPkgIntent.putExtra(PACKAGE_NAME_KEY, pkgName);
            currentPkgPref.setIntent(currentPkgIntent);

            prefScreen.addPreference(currentPkgPref);
        }
        setPreferenceScreen(prefScreen);
    }
}