// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import io.v.android.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.BlessingStore;
import io.v.v23.security.Blessings;
import io.v.v23.security.VPrincipal;
import io.v.v23.security.VSecurity;
import io.v.v23.verror.VException;

/**
 * Provides an interface to store blessings received by various BlesseeRecv activities.
 */
public class StoreBlessingsActivity extends Activity {
    public static final String TAG = "StoreBlessingsActivity";
    public static final String BLESSINGS = "BLESSINGS";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Blessings blessings = (Blessings) getIntent().getExtras().get(BLESSINGS);
            addToBlessingStore(blessings, V.init(this));

            // Display the blessing store.
            Intent intent = new Intent();
            intent.setPackage("io.v.android.apps.account_manager");
            intent.setClassName("io.v.android.apps.account_manager",
                    "io.v.android.apps.account_manager.BlessingStoreDisplayActivity");
            intent.setAction("io.v.android.apps.account_manager.BLESSING_STORE_DISPLAY");
            startActivity(intent);
        } catch (VException e) {
            handleError("Didn't receive blessings");
        }
    }

    private void addToBlessingStore(Blessings blessings, VContext context) throws VException {
        VPrincipal principal = V.getPrincipal(context);
        BlessingStore blessingStore = principal.blessingStore();
        blessingStore.set(blessings, new BlessingPattern(blessings.toString()));
    }

    private void handleError(String error) {
        String msg = "Caveat input error: " + error;
        android.util.Log.e(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }
}
