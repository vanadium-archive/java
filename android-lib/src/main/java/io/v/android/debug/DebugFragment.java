// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.debug;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import io.v.android.VAndroidContext;
import io.v.v23.android.R;

/**
 * Fragment containing a debug menu for common Vanadium/Syncbase debug actions. These actions
 * include:
 * <ul>
 *  <li>{@linkplain DebugLogDialogFragment Change log level and view logcat}
 *  <li>{@linkplain RemoteInspection Remote inspection}
 *  <li>{@linkplain Debug#clearAppData(Context) Clear app data}
 *  <li>{@linkplain Debug#killProcess(Context) Kill process}
 * </ul>
 */
public class DebugFragment extends Fragment {
    public static final String FRAGMENT_TAG = DebugFragment.class.getName();

    public static DebugFragment find(final FragmentManager mgr) {
        return (DebugFragment) mgr.findFragmentByTag(FRAGMENT_TAG);
    }

    private VAndroidContext<?> mVAndroidContext;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    /**
     * This method allows lazy setting/restoration of the {@link VAndroidContext}. This is required
     * because Vanadium contexts are not guaranteed to (and often flat-out do not) persist across
     * activity lifecycle boundaries.
     */
    public void setVAndroidContext(final VAndroidContext vAndroidContext) {
        mVAndroidContext = vAndroidContext;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.debug, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Can't use a switch statement since IDs are not constant in Android libary modules.
        final int id = item.getItemId();
        if (id == R.id.clear_app_data) {
            Debug.clearAppData(getActivity(),
                    mVAndroidContext == null ? null : mVAndroidContext.getErrorReporter());
            return true;
        } else if (id == R.id.kill_process) {
            Debug.killProcess(getActivity());
            return true;
        } else if (id == R.id.logging) {
            new DebugLogDialogFragment().show(getFragmentManager(), null);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
