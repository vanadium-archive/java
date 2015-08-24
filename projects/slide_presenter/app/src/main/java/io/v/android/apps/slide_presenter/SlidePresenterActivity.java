// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.slide_presenter;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.common.collect.ImmutableList;

import java.io.EOFException;

import io.v.android.libs.security.BlessingsManager;
import io.v.android.v23.V;
import io.v.android.v23.services.blessing.BlessingCreationException;
import io.v.android.v23.services.blessing.BlessingService;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.security.VPrincipal;
import io.v.v23.vdl.TypedClientStream;
import io.v.v23.vdl.VdlUint32;
import io.v.v23.verror.VException;
import io.v.x.jni.test.fortune.ComplexErrorParam;

public class SlidePresenterActivity extends Activity {

    static final String BLESSINGS_KEY = "Blessings";
    private static final int BLESSING_REQUEST = 1;
    private static final ComplexErrorParam COMPLEX_PARAM = new ComplexErrorParam(
            "StrVal",
            11,
            ImmutableList.<VdlUint32>of(new VdlUint32(22), new VdlUint32(33)));
    private static final String SERVER_NAME = "users/spetrovic@gmail.com/slidepresenter";
    private static final String TAG = "SlidePresenter";

    /**
     * Specifies how far you need to swipe (up or down) before it
     * will be consider a completed gesture when you lift your finger
     */
    private static final float SWIPE_THRESHOLD_RATIO = 0.35f;

    VContext mBaseContext;
    SlidePresenterClient mClient;
    private GestureDetectorCompat mDetector;
    boolean mStartService = false;
    private final int[] slides = new int[]{R.drawable.slide1, R.drawable.slide2, R.drawable.slide3,
            R.drawable.slide4, R.drawable.slide5, R.drawable.slide6, R.drawable.slide7};
    private int slideNum = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDetector = new GestureDetectorCompat(this, new SlideGestureListener());
        // Initialize the Vanadium runtime and load its native shared library
        // implementation. This is required before we can do anything involving
        // Vanadium.

        mBaseContext = V.init(this);

        // Display slides each as an individual image
        final ImageView mImageView = (ImageView) findViewById(R.id.imageView);
        mImageView.setImageResource(slides[slideNum]);
        mImageView.invalidate();
        getBlessings();
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
                    getBlessings();
                }
                catch (BlessingCreationException e) {
                    String msg = "Couldn't create blessing: " + e.getMessage();
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    android.util.Log.e(TAG, msg);
                } catch (VException e) {
                    String msg = "Couldn't derive blessing: " + e.getMessage();
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    android.util.Log.e(TAG, msg);
                }
                return;
        }
    }

    private void refreshBlessings() {
        Intent intent = BlessingService.newBlessingIntent(this);
        startActivityForResult(intent, BLESSING_REQUEST);
    }

    private void getBlessings() {
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
            // Request new blessings from the account manager.  If successful, this will eventually
            // trigger another call to this method, with BlessingsManager.getBlessings() returning
            // non-null blessings.
            android.util.Log.e(TAG, "Refresh blessings");
            refreshBlessings();
            return;
        }
        try {
            VPrincipal p = V.getPrincipal(mBaseContext);
            p.blessingStore().setDefaultBlessings(blessings);
            p.addToRoots(blessings);
            mClient = SlidePresenterClientFactory.getSlidePresenterClient(SERVER_NAME);

            // Update local state with the new blessings.
            android.util.Log.e(TAG, "Succesfully blessed");
            SlidePresenterAsyncTask slidePresenterAsyncTask = new SlidePresenterAsyncTask();
            slidePresenterAsyncTask.execute();
        } catch (VException e) {
            String msg = String.format(
                    "Couldn't set local blessing %s: %s", blessings, e.getMessage());
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            android.util.Log.e(TAG, msg);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
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
    }

    public void sendSlideNumtoServer(int _slideNum) {
        try {
            mClient.setSlideNum(mBaseContext, _slideNum);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error sending slide # to server:" + e.getMessage());
        }
    }

    public int getSlideNum() {
        return slideNum;
    }

    public int numSlides() {
        return slides.length;
    }

    private class SlideGestureListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";

        @Override
        public boolean onDown(MotionEvent event) {
            Log.d(DEBUG_TAG, "onDown: " + event.toString());
            return true;
        }

        @Override
        public boolean onFling(
                MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {

            int width = findViewById(R.id.imageView).getWidth();

            // Confirm that this is a horizontal (while device is in landscape mode) swipe, then
            // determine if it is a left or right swipe*/
            if (Math.abs(event1.getX() - event2.getX()) / width > SWIPE_THRESHOLD_RATIO) {
                if (event1.getX() > event2.getX()) {
                    setSlideNum(slideNum + 1);
                } else {
                    setSlideNum(slideNum - 1);
                }
                sendSlideNumtoServer(slideNum);
                ((ImageView) findViewById(R.id.imageView)).invalidate();
            }
            return true;
        }
    }

    private class SlidePresenterAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            TypedClientStream<Void, Integer, Void> stream = null;
            try {
                int slideNum = mClient.getSlideNum(mBaseContext);
                stream = mClient.streamingGetSlideNum(mBaseContext);
            } catch (VException e) {
                android.util.Log.e(TAG, "Couldn't create receive stream: " + e.getMessage());
                return null;
            }

            while (true) {
                try {
                    final int streamNum = (Integer) stream.recv();
                    SlidePresenterActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            if (slideNum != streamNum) {
                                setSlideNum(streamNum);
                                ((ImageView) findViewById(R.id.imageView)).invalidate();
                            }
                        }
                    });
                } catch (EOFException e) {
                    android.util.Log.e(TAG, "Sender closed stream!");
                    break;
                } catch (VException e) {
                    android.util.Log.e(TAG, "Error receiving slide number: " + e.getMessage());
                }
            }
            return null;
        }
    }
}
