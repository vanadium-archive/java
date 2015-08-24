// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.account_manager;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.security.interfaces.ECPublicKey;
import java.util.Map;

import io.v.android.v23.V;
import io.v.android.v23.services.blessing.BlessingService;
import io.v.v23.context.VContext;
import io.v.v23.security.BlessingPattern;
import io.v.v23.security.Blessings;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * Mints a new set of Vanadium {@link Blessings} for the invoking application.
 */
public class LocalBlessingActivity extends Activity {
    public static final String TAG = "LocalBlessingActivity";

    private static final int REQUEST_CODE_BLESS = 1000;
    private static final int REQUEST_CODE_CREATE_ACCOUNT = 1001;

    private VContext mBaseContext;
    private String mBlesseePkgName;
    private ECPublicKey mBlesseePubKey;
    private String mGoogleAccount;
    private byte[] mBlessWithVom = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBaseContext = V.init(this);
        // Get the name of the application invoking this activity.
        Intent intent = getIntent();
        if (intent == null || intent.getExtras() == null) {
            replyWithError("No extras provided.");
            return;
        }

        // Get blessee package name.
        mBlesseePkgName = getCallingActivity().getPackageName();
        if (mBlesseePkgName == null || mBlesseePkgName.isEmpty()) {
            replyWithError("Empty blessee package name.");
            return;
        }

        // Get blessee public key.
        mBlesseePubKey =
                (ECPublicKey) intent.getSerializableExtra(BlessingService.EXTRA_PUBLIC_KEY);
        if (mBlesseePubKey == null) {
            replyWithError("Empty blessee public key.");
            return;
        }

        // See if the user wants to base the blessings on the specific google account.
        // If so, we will try to look for it in the list of existing blessings and mint a new
        // set of blessings if necessary.
        mGoogleAccount = intent.getStringExtra(BlessingService.EXTRA_GOOGLE_ACCOUNT);
        if (mGoogleAccount != null && !mGoogleAccount.isEmpty()) {
            handleWithBlessings(false);
            return;
        }
        bless();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CREATE_ACCOUNT:
                if (resultCode != RESULT_OK) {
                    replyWithError("Couldn't create a Vanadium blessing.");
                    return;
                }
                handleWithBlessings(true);
                break;
            case REQUEST_CODE_BLESS:
                if (resultCode != RESULT_OK) {
                    replyWithError("Blessing error: " + data.getStringExtra(Constants.ERROR));
                    return;
                }
                replyWithSuccess(data.getByteArrayExtra(Constants.REPLY));
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void handleWithBlessings(boolean postCreation) {
        // Attempt to find the blessing matching the specified google account name.
        String blessingName = "dev.v.io/u/" + mGoogleAccount + "/android";
        Map<BlessingPattern, Blessings> allBlessings =
                V.getPrincipal(mBaseContext).blessingStore().peerBlessings();
        Blessings blessings = allBlessings.get(new BlessingPattern(blessingName));
        if (blessings == null) {
            // Blessings with the given name don't exist: create them.
            if (postCreation) {  // to avoid the infinite creation loop
                replyWithError(
                        "Couldn't find already created blessings with name: " + blessingName);
                return;
            }
            Intent intent = new Intent(this, AccountActivity.class);
            intent.putExtra(AccountActivity.GOOGLE_ACCOUNT, mGoogleAccount);
            startActivityForResult(intent, REQUEST_CODE_CREATE_ACCOUNT);
            return;
        }
        try {
            mBlessWithVom = VomUtil.encode(blessings, Blessings.class);
            bless();
        } catch (VException e) {
            replyWithError("Couldn't encode blessings: " + e.getMessage());
            return;
        }
    }

    private void bless() {
        Intent i = new Intent(this, BlessActivity.class);
        i.putExtra(BlessActivity.BLESSEE_PUBLIC_KEY, mBlesseePubKey);
        i.putExtra(BlessActivity.BLESSEE_NAMES, new String[]{mBlesseePkgName});
        i.putExtra(BlessActivity.BLESSEE_EXTENSION, mBlesseePkgName);
        i.putExtra(BlessActivity.BLESSEE_EXTENSION_MUTABLE, false);
        i.putExtra(BlessActivity.BLESS_WITH, mBlessWithVom);
        startActivityForResult(i, REQUEST_CODE_BLESS);
    }

    private void replyWithError(String error) {
        android.util.Log.e(TAG, "Blessing error: " + error);
        Intent intent = new Intent();
        intent.putExtra(BlessingService.EXTRA_ERROR, error);
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    private void replyWithSuccess(byte[] blessingsVom) {
        Intent intent = new Intent();
        intent.putExtra(BlessingService.EXTRA_REPLY, blessingsVom);
        setResult(RESULT_OK, intent);
        finish();
    }
}
