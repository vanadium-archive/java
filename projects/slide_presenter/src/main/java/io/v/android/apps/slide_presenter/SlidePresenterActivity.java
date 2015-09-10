// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.slide_presenter;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.EOFException;

import io.v.android.libs.security.BlessingsManager;
import io.v.android.v23.V;
import io.v.android.v23.services.blessing.BlessingCreationException;
import io.v.android.v23.services.blessing.BlessingService;
import io.v.v23.context.VContext;
import io.v.v23.security.Blessings;
import io.v.v23.security.VPrincipal;
import io.v.v23.vdl.TypedClientStream;
import io.v.v23.verror.VException;
import io.v.v23.vom.VomUtil;

public class SlidePresenterActivity extends Activity {

    private static final int BLESSING_REQUEST = 1;
    private static final String SERVER_NAME = "users/spetrovic@gmail.com/slidepresenter";
    private static final String TAG = "SlidePresenter";
    VContext mBaseContext;
    SlidePresenterClient mClient;
    private final int[] slideDrawables = new int[]{R.drawable.slide1, R.drawable.slide2, R.drawable.slide3,
            R.drawable.slide4, R.drawable.slide5, R.drawable.slide6, R.drawable.slide7};
    private int slideNum = 0, localSlideNum = 0;
    private Slide[] slides = new Slide[slideDrawables.length];
    private boolean synced = true;

    private enum Role {
        Lecturer, Moderator, Audience
    }

    private Role role = Role.Audience;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (role == Role.Lecturer) {
            setContentView(R.layout.activity_slidelecturer);
        } else if (role == Role.Audience) {
            setContentView(R.layout.activity_slideaudience);
        }

        // Initialize the Vanadium runtime and load its native shared library
        // implementation. This is required before we can do anything involving
        // Vanadium.
        mBaseContext = V.init(this);

        for (int i = 0; i < slideDrawables.length; i++) {
            slides[i] = new Slide(slideDrawables[i]);
            slides[i] = new Slide(slideDrawables[i], "Notes for slide " + (i + 1));
        }

        setSlideNum(0);
        getBlessings();

        if (role == Role.Audience) {
            findViewById(R.id.prevButton).setVisibility(View.INVISIBLE);
            findViewById(R.id.nextButton).setVisibility(View.INVISIBLE);
        }
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
                } catch (BlessingCreationException e) {
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
        final int thisSlide = _slideNum < 0 ? 0
                            : _slideNum >= slides.length ? slides.length - 1
                            : _slideNum;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((ImageView) findViewById(R.id.slideView))
                        .setImageResource(slides[thisSlide].getSlideDrawableId());

                ((ImageView) findViewById(R.id.currSlide))
                        .setImageResource(slides[thisSlide].getSlideDrawableId());

                if (thisSlide > 0) {
                    ((ImageView) findViewById(R.id.prevSlide))
                            .setImageResource(slides[thisSlide - 1].getSlideDrawableId());
                } else {
                    ((ImageView) findViewById(R.id.prevSlide)).setImageResource(0);
                }

                if (thisSlide < slides.length - 1) {
                    ((ImageView) findViewById(R.id.nextSlide))
                            .setImageResource(slides[thisSlide + 1].getSlideDrawableId());
                } else {
                    ((ImageView) findViewById(R.id.nextSlide)).setImageResource(0);
                }

                if (role == Role.Lecturer || role == Role.Moderator) {
                    slideNum = thisSlide;
                    ((TextView) findViewById(R.id.textView)).setText(slides[slideNum].getSlideNotes());
                } else {
                    localSlideNum = thisSlide;
                }

                findViewById(R.id.slideView).invalidate();
            }
        });

    }

    public void sendSlideNumtoServer(int _slideNum) {
        try {
            mClient.setSlideNum(mBaseContext, _slideNum);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error sending slide # to server:" + e.getMessage());
        }
    }

    public void nextSlide(View v) {
        if (role == Role.Lecturer || role == Role.Moderator) {
            setSlideNum(slideNum + 1);
            sendSlideNumtoServer(slideNum);
        } else if (role == Role.Audience) {
            setSlideNum(localSlideNum + 1);
        }

    }

    public void prevSlide(View v) {
        if (role == Role.Lecturer || role == Role.Moderator) {
            setSlideNum(slideNum - 1);
            sendSlideNumtoServer(slideNum);
        } else if (role == Role.Audience) {
            setSlideNum(localSlideNum - 1);
        }
    }

    public void syncClick(View v) {
        synced = !synced;

        if (synced) {
            findViewById(R.id.prevButton).setVisibility(View.INVISIBLE);
            findViewById(R.id.nextButton).setVisibility(View.INVISIBLE);
            ((Button) findViewById(R.id.syncButton)).setText("Unsync");
        } else {
            findViewById(R.id.prevButton).setVisibility(View.VISIBLE);
            findViewById(R.id.nextButton).setVisibility(View.VISIBLE);
            ((Button) findViewById(R.id.syncButton)).setText("Sync");
        }
    }

    public int getSlideNum() {
        return slideNum;
    }

    public int numSlides() {
        return slides.length;
    }

    private class SlidePresenterAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            TypedClientStream<Void, Integer, Void> stream = null;
            try {
                stream = mClient.streamingGetSlideNum(mBaseContext);
            } catch (VException e) {
                android.util.Log.e(TAG, "Couldn't create receive stream: " + e.getMessage());
                return null;
            }

            while (true) {
                try {
                    final int streamNum;
                    streamNum = stream.recv();
                    if (slideNum != streamNum && synced) {
                        setSlideNum(streamNum);
                    }
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

    private class Slide {
        int slideDrawableId;
        String slideNotes;

        public Slide() {
            slideDrawableId = 0;
            slideNotes = "";
        }

        public Slide(int _slideDrawableId) {
            slideDrawableId = _slideDrawableId;
        }

        public Slide(int _slideDrawableId, String _slideNotes) {
            slideDrawableId = _slideDrawableId;
            slideNotes = _slideNotes;
        }

        public int getSlideDrawableId() {
            return slideDrawableId;
        }

        public String getSlideNotes() {
            return slideNotes;
        }
    }
}

