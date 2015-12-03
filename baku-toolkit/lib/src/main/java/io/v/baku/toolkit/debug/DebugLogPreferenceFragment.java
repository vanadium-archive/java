// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.debug;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.v.baku.toolkit.R;
import io.v.baku.toolkit.VAndroidContextTrait;
import io.v.baku.toolkit.VOptionPreferenceUtils;
import io.v.v23.OptionDefs;

/**
 * A {@link PreferenceFragment} surfacing logging options.
 * <ul>
 * <li>VLEVEL - verbosity for vlog</li>
 * <li>VMODULE - per-module log verbosities</li>
 * </ul>
 */
public class DebugLogPreferenceFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final PreferenceManager pm = getPreferenceManager();
        pm.setSharedPreferencesName(VAndroidContextTrait.VANADIUM_OPTIONS_SHARED_PREFS);
        pm.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        createPreferences();
    }

    private void createPreferences() {
        addPreferencesFromResource(R.xml.pref_logging);
        final SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        updateVLevelSummary(sharedPreferences, findPreference(OptionDefs.LOG_VLEVEL));
        updateVModuleSummary(sharedPreferences, findPreference(OptionDefs.LOG_VMODULE));
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        view.setBackgroundColor(getResources().getColor(android.R.color.background_light));

        return view;
    }

    private static void updateVLevelSummary(final SharedPreferences sharedPreferences,
                                            final Preference pref) {
        pref.setSummary(VOptionPreferenceUtils.readVLevel(sharedPreferences)
                .map(Object::toString)
                .orElse(null));
    }

    private static void updateVModuleSummary(final SharedPreferences sharedPreferences,
                                             final Preference pref) {
        pref.setSummary(VOptionPreferenceUtils.readVModule(sharedPreferences).orElse(null));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        final Preference pref = findPreference(key);
        if (pref == null) {
            return;
        }

        if (OptionDefs.LOG_VLEVEL.equals(key)) {
            updateVLevelSummary(sharedPreferences, pref);
        } else if (OptionDefs.LOG_VMODULE.equals(key)) {
            updateVModuleSummary(sharedPreferences, pref);
        }
    }

    public void reset() {
        getPreferenceManager().getSharedPreferences().edit()
                .clear()
                .commit();
        // The easiest way to resync the preferences seems to be:
        getPreferenceScreen().removeAll();
        createPreferences();
    }
}
