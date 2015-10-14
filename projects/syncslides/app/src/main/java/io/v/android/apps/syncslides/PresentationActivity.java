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

import io.v.android.apps.syncslides.db.DB;

public class PresentationActivity extends AppCompatActivity {

    private static final String TAG = "PresentationActivity";
    public static final String DECK_ID_KEY = "deck_id";
    public static final String ROLE_KEY = "role";

    private String mDeckId;
    /**
     * The current role of the user.  This value can change during the lifetime
     * of the activity.
     */
    private Role mRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Do this initialization early on in case it needs to start the AccountManager.
        DB.Singleton.get(getApplicationContext()).init(this);

        setContentView(R.layout.activity_presentation);

        Bundle bundle = savedInstanceState;
        if (bundle == null) {
            bundle = getIntent().getExtras();
        }
        mDeckId = bundle.getString(DECK_ID_KEY);
        mRole = (Role) bundle.get(ROLE_KEY);

        if (savedInstanceState != null) {
            return;
        }

        SlideListFragment slideList = SlideListFragment.newInstance(mDeckId, mRole);
        getSupportFragmentManager().beginTransaction().add(R.id.fragment, slideList).commit();

        // If this is an audience member, we want them to jump straight to the fullscreen view.
        if (mRole == Role.AUDIENCE) {
            // TODO(kash): The back button will take the AUDIENCE member
            //    FullscreenSlide --> Navigate --> SlideList --> DeckChooser
            // It would be better if it went
            //    FullscreenSlide --> Navigate --> DeckChooser
            // I tried to get this to work, but it was too much trouble.  We need to
            // inspect the back stack to get it right.
            jumpToSlideSynced(0);
            fullscreenSlide(0);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TODO(jregan): Stop advertising the live presentation if necessary.
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(DECK_ID_KEY, mDeckId);
        outState.putSerializable(ROLE_KEY, mRole);
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
     *
     * @param slideNum the slide to show
     */
    public void jumpToSlideSynced(int slideNum) {
        jumpToSlide(slideNum, true);
    }

    /**
     * Swap out the current fragment for a NavigateFragment. Audience members who click on a slide
     * from the slide list will start unsynced.
     *
     * @param slideNum the slide to show
     */
    public void jumpToSlideUnsynced(int slideNum) {
        jumpToSlide(slideNum, false);
    }

    private void jumpToSlide(int slideNum, boolean synced) {
        NavigateFragment fragment = NavigateFragment.newInstance(
                mDeckId, slideNum, mRole, synced);
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
        mRole = Role.PRESENTER;
        jumpToSlideSynced(0);
    }

    /**
     * Adds the FullscreenSlideFragment to the main container and back stack.
     *
     * @param slideNum the number of the slide to show full screen
     */
    public void fullscreenSlide(int slideNum) {
        FullscreenSlideFragment fullscreenSlideFragment =
                FullscreenSlideFragment.newInstance(mDeckId, slideNum, mRole);
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.fragment, fullscreenSlideFragment)
                .addToBackStack("")
                .commit();
    }
}
