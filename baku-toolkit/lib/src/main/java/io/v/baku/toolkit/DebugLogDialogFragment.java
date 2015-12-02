// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * A {@link DialogFragment} surfacing logging options via a {@link DebugLogPreferenceFragment},
 * Reset and Restart action buttons, and logcat via a {@link LogCatFragment}.
 */
public class DebugLogDialogFragment extends DialogFragment {
    private static final String DEBUG_LOG_PF_TAG = "DebugLogPreferences";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle(R.string.logging);
        return dialog;
    }

    private void wireInEventHandlers(final View view) {
        final Button reset = (Button) view.findViewById(R.id.debug_log_reset);
        if (reset != null) {
            reset.setOnClickListener(v -> ((DebugLogPreferenceFragment) getChildFragmentManager()
                    .findFragmentByTag(DEBUG_LOG_PF_TAG)).reset());
        }

        final Button restart = (Button) view.findViewById(R.id.debug_log_restart);
        if (restart != null) {
            restart.setOnClickListener(v -> DebugUtils.restartProcess(getActivity()));
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.dialog_debug_log, container);
        wireInEventHandlers(view);
        return view;
    }

    private void wireInSubFragments(final Bundle savedInstanceState) {
        final FragmentTransaction ft = getChildFragmentManager().beginTransaction();

        if (getView().findViewById(R.id.pref_logging_container) != null &&
                getChildFragmentManager().findFragmentByTag(DEBUG_LOG_PF_TAG) == null) {
            ft.replace(R.id.pref_logging_container,
                    new DebugLogPreferenceFragment(),
                    DEBUG_LOG_PF_TAG);
        }
        if (savedInstanceState == null) {
            ft.replace(R.id.logcat_container, new LogCatFragment());
        }
        ft.commit();
    }

    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        wireInSubFragments(savedInstanceState);
    }
}
