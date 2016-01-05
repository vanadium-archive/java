// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.moments.ux;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.google.common.util.concurrent.Uninterruptibles;

import java.util.concurrent.TimeUnit;

import io.v.moments.R;
import io.v.moments.ifc.Moment;
import io.v.moments.model.BitMapper;
import io.v.moments.model.Config;

/**
 * A full screen activity to show a photo.
 */
public class ShowPhotoActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "ShowPhotoActivity";
    private final Handler mHideHandler = new Handler();
    private ImageView mImageView;
    private View mControlView;
    private boolean mIsNavControlVisible;
    private final Runnable mRunnableHide = new Runnable() {
        @Override
        public void run() {
            hideNavControl();
        }
    };

    public static Intent makeIntent(
            Context context, int photoIndex, Moment.Kind kind) {
        Intent intent = new Intent(context, ShowPhotoActivity.class);
        intent.putExtra(Extra.PHOTO_INDEX, photoIndex);
        intent.putExtra(Extra.MOMENT_KIND, kind.toString());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_photo);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mIsNavControlVisible = true;
        mControlView = findViewById(R.id.fullscreen_content_controls);
        mImageView = (ImageView) findViewById(R.id.moment_image);
        mImageView.setImageBitmap(loadBitmap(getIntent()));
        mImageView.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (mIsNavControlVisible) {
            hideNavControl();
        } else {
            showNavControl();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Hide controls after activity has been created, showing them just
        // long enough to let the user know they are there.
        mHideHandler.removeCallbacks(mRunnableHide);
        mHideHandler.postDelayed(mRunnableHide, 500);
    }

    private void legacyHideControls() {
        mImageView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LOW_PROFILE
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private Bitmap loadBitmap(Intent intent) {
        int index = intent.getIntExtra(Extra.PHOTO_INDEX, 0);
        Moment.Kind kind = Moment.Kind.valueOf(
                intent.getStringExtra(Extra.MOMENT_KIND));
        Log.d(TAG, "Showing " + kind + " photo index " + index);
        BitMapper bitMapper = Config.makeBitmapper(this);

        int attempts = 0;
        while (attempts < 3) {
            try {
                attempts++;
                return bitMapper.readFullWithFallback(index, kind);
            } catch (Exception e) {
                Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
            }
        }
        /** Camera result not ready.  See {@link BitMapper.dealWithCameraResult} */
        throw new IllegalStateException("Unable to read file index " + index);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void hideNavControl() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlView.setVisibility(View.GONE);
        mIsNavControlVisible = false;
        legacyHideControls();
    }

    @SuppressLint("InlinedApi")
    private void showNavControl() {
        mImageView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mIsNavControlVisible = true;
        mControlView.setVisibility(View.VISIBLE);
    }

    static class Extra {
        static final String PHOTO_INDEX = "photo_index";
        static final String MOMENT_KIND = "moment_kind";
    }
}
