// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.security.interfaces.ECPublicKey;

import io.v.v23.security.Blessings;

/**
 * Mints a new set of Vanadium {@link Blessings} for the invoking application.
 */
public class LocalBlessingActivity extends Activity {
    public static final String TAG = "LocalBlessingActivity";
    public static final String BLESSEE_PUBKEY_KEY = "BLESSEE_PUBKEY";
    public static final String ERROR = "ERROR";

    private static final int BLESS_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Get the name of the application invoking this activity.
        Intent intent = getIntent();
        if (intent == null || intent.getExtras() == null) {
            replyWithError("No extras provided.");
            return;
        }

        // Get callee package name.
        String blesseePkgName = getCallingActivity().getPackageName();
        if (blesseePkgName == null || blesseePkgName.isEmpty()) {
            replyWithError("Empty blessee package name.");
            return;
        }

        // Get the public key of the application invoking this activity.
        Bundle extras = getIntent().getExtras();
        ECPublicKey blesseePubKey = (ECPublicKey) extras.getSerializable(BLESSEE_PUBKEY_KEY);
        if (blesseePubKey == null) {
            replyWithError("Empty blessee public key.");
            return;
        }

        // Bless the calling Vanadium app.
        Intent i = new Intent(this, BlessActivity.class);
        i.putExtra(BlessActivity.BLESSEE_PUBLIC_KEY, blesseePubKey);
        i.putExtra(BlessActivity.BLESSEE_NAMES, new String[]{blesseePkgName});
        i.putExtra(BlessActivity.BLESSEE_EXTENSION, blesseePkgName);
        i.putExtra(BlessActivity.BLESSEE_EXTENSION_MUTABLE, false);
        startActivityForResult(i, BLESS_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BLESS_REQUEST:
                setResult(resultCode, data);
                finish();
                return;
        }
    }

    private void replyWithError(String error) {
        android.util.Log.e(TAG, "Blessing error: " + error);
        Intent intent = new Intent();
        intent.putExtra(ERROR, error);
        setResult(RESULT_CANCELED, intent);
        finish();
    }
}
