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
import io.v.android.v23.services.blessing.BlessingCreationException;
import io.v.android.v23.services.blessing.BlessingService;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

/**
 * To install on a device:
 *
 *   cd $JIRI_ROOT/vanadium/release/java/projects/location
 *   gradle assembleDebug
 *   adb install -r build/outputs/apk/location-debug.apk
 *
 * At runtime, the location service is mounted on the public dev.v.io table.
 *
 * To access it from a command line, first get a blessing from dev.v.io.
 * Assuming you've done a standard install (see instructions at v.io) enter:
 *
 *   $V_BIN/principal --v23.credentials /tmp/creds seekblessings -browser=false
 *
 * Visit the URL using a browser logged into an account that matches the account
 * used on your device, and click BLESS.  This installs a blessing on your
 * computer that can be used to access your phone's location service.
 *
 * For convenience, define a service name env variable:
 *
 *   serviceName=users/${YOUR_EMAIL}/android/location
 *
 * List the service:
 *
 *   $V_BIN/namespace --v23.credentials /tmp/creds glob -l $serviceName
 *
 * Query its signature:
 *
 *   $V_BIN/vrpc --v23.credentials /tmp/creds signature $serviceName
 *
 * Obtain the phone's location:
 *
 *   $V_BIN/vrpc --v23.credentials /tmp/creds call $serviceName get
 *
 */
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
        Intent intent = BlessingService.newBlessingIntent(this);
        startActivityForResult(intent, BLESSING_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BLESSING_REQUEST:
                try {
                    byte[] blessingsVom = BlessingService.extractBlessingReply(resultCode, data);
                    Blessings blessings = (Blessings) VomUtil.decode(blessingsVom, Blessings.class);
                    BlessingsManager.addBlessings(this, blessings);
                    Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show();
                    if (mStartService) {
                        startLocationService(blessings);
                    }
                } catch (BlessingCreationException e) {
                    String msg = "Couldn't create blessing: " + e.getMessage();
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    android.util.Log.e(TAG, msg);
                } catch (VException e) {
                    String msg = "Couldn't store blessing: " + e.getMessage();
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
