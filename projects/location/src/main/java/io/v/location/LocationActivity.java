// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.location;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import io.v.android.libs.security.BlessingsManager;
import io.v.android.v23.V;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

public class LocationActivity extends Activity {
    private static final String TAG = "LocationActivity";
    private static final int BLESSING_REQUEST = 1;
    static final String BLESSINGS_KEY = "Blessings";

    boolean mStartService = false;
    VContext mBaseContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        mBaseContext = V.init(this);
        Drawable d = getResources().getDrawable(R.drawable.ic_account_box_black_36dp);
        d.setColorFilter(new LightingColorFilter(Color.BLACK, Color.GRAY));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public void onStartService(View v) {
        Blessings blessings = null;
        try {
            // See if there are blessings stored in shared preferences.
            blessings = BlessingsManager.getBlessings(this);
        } catch (VException e) {
            String msg = "Error getting blessings from shared preferences " + e.getMessage();
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            android.util.Log.e(TAG, msg);
        }
        if (blessings == null) {
            // Request new blessings from the account manager.  If successful, this will
            // start the service with the newly obtained blessings.
            fetchBlessings(true);
            return;
        }
        startLocationService(blessings);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_account: {
                fetchBlessings(false);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void fetchBlessings(boolean startService) {
        mStartService = startService;
        Intent intent = BlessingsManager.newRefreshBlessingsIntent(this);
        startActivityForResult(intent, BLESSING_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BLESSING_REQUEST:
                try {
                    Blessings blessings = BlessingsManager.processBlessingsReply(resultCode, data);
                    BlessingsManager.addBlessings(this, blessings);
                    Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
                    if (mStartService) {
                        startLocationService(blessings);
                    }
                } catch (VException e) {
                    String msg = "Couldn't derive blessing: " + e.getMessage();
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    android.util.Log.e(TAG, msg);
                }
                return;
        }
    }

    private void startLocationService(Blessings blessings) {
        try {
            String blessingsVom = VomUtil.encodeToString(blessings, Blessings.class);
            Intent intent = new Intent(this, LocationService.class);
            intent.putExtra(BLESSINGS_KEY, blessingsVom);
            stopService(intent);
            startService(intent);
        } catch (VException e) {
            String msg = String.format(
                    "Couldn't encode blessings %s: %s", blessings, e.getMessage());
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            android.util.Log.e(TAG, msg);
        }
    }
}
