// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;

public class SettingsActivity extends PreferenceActivity {
    public static final String IDENTITY_SERVICE_NAME = "IDENTITY_SERVICE_NAME";

    EditTextPreference mIdServicePref;
    Preference.OnPreferenceChangeListener mPrefChangeListener =
            new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    mIdServicePref.setSummary((String) newValue);
                    return true;
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.vanadium_preferences);
        PreferenceScreen prefScreen = getPreferenceScreen();
        mIdServicePref = new EditTextPreference(this);
        mIdServicePref.setKey(IDENTITY_SERVICE_NAME);
        mIdServicePref.setOnPreferenceChangeListener(mPrefChangeListener);
        mIdServicePref.setDefaultValue(Constants.IDENTITY_DEV_V_IO_U_GOOGLE);
        mIdServicePref.setTitle("Vanadium Identity Service Name");
        mIdServicePref.setDialogTitle(mIdServicePref.getTitle());
        mIdServicePref.setSummary(mIdServicePref.getText());
        prefScreen.addPreference(mIdServicePref);
    }
}