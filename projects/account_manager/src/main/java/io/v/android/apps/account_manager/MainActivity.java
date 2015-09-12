// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Vanadium app launcher.
 */
public class MainActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void showGrantedBlessingsLogs(MenuItem item) {
        Intent intent = new Intent();
        intent.setPackage("io.v.android.apps.account_manager");
        intent.setClassName("io.v.android.apps.account_manager",
                "io.v.android.apps.account_manager.BlessedPrincipalsDisplayActivity");
        intent.setAction("io.v.android.apps.account_manager.BLESSED_PRINCIPALS_DISPLAY");
        startActivity(intent);
    }

    public void getIdentityBlessing(MenuItem item) {
        Intent intent = new Intent();
        intent.setPackage("io.v.android.apps.account_manager");
        intent.setClassName("io.v.android.apps.account_manager",
                "io.v.android.apps.account_manager.AccountActivity");
        startActivity(intent);
    }
}
