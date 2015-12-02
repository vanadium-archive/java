// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.baku.toolkit;

import android.app.Fragment;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DebugFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.debug, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Can't use a switch statement since IDs are not constant in Android libary modules.
        final int id = item.getItemId();
        if (id == R.id.clear_app_data) {
            DebugUtils.clearAppData(getActivity());
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
