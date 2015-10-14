// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import io.v.android.apps.syncslides.db.DB;

public class PresentationActivity extends AppCompatActivity {

    private static final String TAG = "PresentationActivity";
    public static final String DECK_ID_KEY = "deck_id";

    private String mDeckId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presentation);

        Bundle bundle = getIntent().getExtras();
        mDeckId = bundle.getString(DECK_ID_KEY);

        if (savedInstanceState == null) {
            SlideListFragment slideList = SlideListFragment.newInstance(mDeckId);
            getSupportFragmentManager().beginTransaction().add(R.id.fragment, slideList).commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TODO(jregan): Stop advertising the live presentation if necessary.
    }

    /**
     * Set the system UI to be immersive or not.
     */
    public void setUiImmersive(boolean immersive) {
        if (immersive) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            // See the comment at the top of fragment_slide_list.xml for why we don't simply
            // use View.SYSTEM_UI_FLAG_VISIBLE.
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    /**
     * Swap out the current fragment for a NavigateFragment. The presenter and audience members
     * who join live presentations will be synced.
     * @param slideNum the slide to show
     */
    public void jumpToSlideSynced(int slideNum) {
        jumpToSlide(slideNum, true);
    }

    /**
     * Swap out the current fragment for a NavigateFragment. Audience members who click on a slide
     * from the slide list will start unsynced.
     * @param slideNum the slide to show
     */
    public void jumpToSlideUnsynced(int slideNum) {
        jumpToSlide(slideNum, false);
    }

    private void jumpToSlide(int slideNum, boolean synced) {
        NavigateFragment fragment = NavigateFragment.newInstance(
                mDeckId, slideNum, NavigateFragment.Role.AUDIENCE, synced);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, fragment)
                .addToBackStack("")
                .commit();
    }

    /**
     * Starts a live presentation.  The presentation will be advertised to other
     * devices as long as this activity is alive.
     */
    public void startPresentation() {
        DB db = DB.Singleton.get(getApplicationContext());
        db.createPresentation(mDeckId, new DB.Callback<DB.StartPresentationResult>() {
            @Override
            public void done(DB.StartPresentationResult startPresentationResult) {
                Log.i(TAG, "Started presentation");
                Toast.makeText(getApplicationContext(), "Started presentation",
                        Toast.LENGTH_SHORT).show();
                // TODO(jregan): Advertise this presentation.
            }
        });
    }
}
