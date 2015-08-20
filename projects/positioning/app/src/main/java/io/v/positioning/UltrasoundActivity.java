// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.positioning;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class UltrasoundActivity extends Activity {
    private static final String TAG = UltrasoundActivity.class.getSimpleName();
    private UltrasoundDetectorAsyncTask mUltrasoundDetector = null;
    private UltrasoundGenerator mUltrasoundGenerator = null;
    private boolean mReceiving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ultrasound);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ultrasound, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    public void onSendUltrasound(View view) {
        mUltrasoundGenerator = new UltrasoundGenerator();
        long timeSoundPlayed = mUltrasoundGenerator.playUltrasound();
        Log.d(TAG, "ultrasound played at " + timeSoundPlayed);
    }

    public void onToggleReceivingUltrasound(View view) {
        if (!mReceiving) {
            mReceiving = true;
            mUltrasoundDetector = new UltrasoundDetectorAsyncTask();
            mUltrasoundDetector.execute(this);
            ((Button) view.findViewById(R.id.receiving_ultrasound)).setText(R.string.stop_receiving_ultrasound);
            Log.d(TAG, "start receiving US");
        } else {
            mReceiving = false;
            if (mUltrasoundDetector != null) {
                mUltrasoundDetector.cancel(true);
            }
            ((Button) view.findViewById(R.id.receiving_ultrasound)).setText(R.string.start_receiving_ultrasound);
            Log.d(TAG, "stopped receiving US");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mUltrasoundDetector != null) {
            mUltrasoundDetector.cancel(true);
        }
        if (mUltrasoundGenerator != null) {
            mUltrasoundGenerator.stopUltrasound();
        }
    }
}
