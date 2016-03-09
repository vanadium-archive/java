// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit.debug;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import io.v.baku.toolkit.R;
import io.v.baku.toolkit.VAndroidContextTrait;
import lombok.extern.slf4j.Slf4j;

/**
 * Fragment containing a debug menu for common Vanadium/Syncbase debug actions. These actions
 * include:
 *
 * * {@linkplain DebugLogDialogFragment Change log level and view logcat}
 * * {@linkplain RemoteInspection Remote inspection}
 * * {@linkplain DebugUtils#clearAppData(Context) Clear app data}
 * * {@linkplain DebugUtils#killProcess(Context) Kill process}
 */
@Slf4j
public class DebugFragment extends Fragment {
    public static final String DEBUG_SHARED_PREFS = "VanadiumDebugOptions";

    private RemoteInspection mRemoteInspection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (getActivity() instanceof VAndroidContextTrait) {
            mRemoteInspection = new RemoteInspection((VAndroidContextTrait<?>)getActivity(),
                    getActivity().getSharedPreferences(DEBUG_SHARED_PREFS, Context.MODE_PRIVATE));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Don't use a stale one if we're later resurrected but unable to instantiate.
        mRemoteInspection = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.debug, menu);

        // Only allow remote inspection if we were able to instantiate the utility during onCreate.
        if (mRemoteInspection == null) {
            menu.findItem(R.id.inspect).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Can't use a switch statement since IDs are not constant in Android libary modules.
        final int id = item.getItemId();
        if (id == R.id.clear_app_data) {
            DebugUtils.clearAppData(getActivity());
            return true;
        } else if (id == R.id.inspect) {
            mRemoteInspection.showDialog();
            return true;
        } else if (id == R.id.kill_process) {
            DebugUtils.killProcess(getActivity());
            return true;
        } else if (id == R.id.logging) {
            new DebugLogDialogFragment().show(getFragmentManager(), null);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}
