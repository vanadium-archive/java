// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.slide_presenter;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.ImageView;

import io.v.v23.android.V;

public class MainActivity extends Activity {

    /**These constants were inspired by
     * https://android.googlesource.com/platform/frameworks/base/+/2a114bdc64a33df509abb84de1a730ed3be49119/core/java/android/widget/StackView.java
     */

    /**
     * Specifies how far you need to swipe (up or down) before it
     * will be consider a completed gesture when you lift your finger
     */
    private static final float SWIPE_THRESHOLD_RATIO = 0.35f;
    /**
     * Specifies the total distance, relative to the size of the stack,
     * that views will be slid, either up or down
     */
    private static final float SLIDE_UP_RATIO = 0.7f;

    private final int[] slides = new int[]{R.drawable.slide1, R.drawable.slide2, R.drawable.slide3,
            R.drawable.slide4, R.drawable.slide5, R.drawable.slide6, R.drawable.slide7};
    private int slideNum = 0;
    private GestureDetectorCompat mDetector;
    Handler mHandler;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDetector = new GestureDetectorCompat(this, new SlideGestureListener());

        // Initialize the Vanadium runtime and load its native shared library
        // implementation. This is required before we can do anything involving
        // Vanadium.
        //VContext context = V.init();
        V.init(this);

        //Display slides each as an individual image
        ImageView mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setImageResource(slides[slideNum]);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    class SlideGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";

        @Override
        public boolean onDown(MotionEvent event) {
            Log.d(DEBUG_TAG, "onDown: " + event.toString());
            return true;
        }

        @Override
        public boolean onFling(MotionEvent event1, MotionEvent event2,
                               float velocityX, float velocityY) {
            Log.d(DEBUG_TAG, "onFling: " + event1.toString() + event2.toString() + "," + velocityX
                    + "," + velocityY);

            int width = findViewById(R.id.imageView).getWidth();

            /**Confirm that this is a horizontal (while device is in landscape mode) swipe, then
             determine if it is a left or right swipe*/
            if (Math.abs(event1.getX() - event2.getX()) / width > SWIPE_THRESHOLD_RATIO) {
                if (event1.getX() > event2.getX()) {
                    setSlideNum(slideNum + 1);
                } else {
                    setSlideNum(slideNum - 1);
                }
            }
            return true;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setSlideNum(int _slideNum) {
        slideNum = _slideNum;
        if (slideNum < 0) {
            slideNum = 0;
        } else if (slideNum >= slides.length) {
            slideNum = slides.length - 1;
        }

        ImageView mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setImageResource(slides[slideNum]);
        //mImageView.invalidate();
    }

    public int getSlideNum() {

        return slideNum;
    }

    public int numSlides() {
        return slides.length;
    }
}

