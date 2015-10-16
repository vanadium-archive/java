// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;


import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import io.v.android.apps.syncslides.db.DB;
import io.v.android.apps.syncslides.discovery.ParticipantPeer;
import io.v.android.apps.syncslides.model.Deck;
import io.v.android.apps.syncslides.model.DeckImpl;
import io.v.android.apps.syncslides.model.Participant;

public class PresentationActivity extends AppCompatActivity {
    private static final String TAG = "PresentationActivity";

    /**
     * The deck to present.
     */
    private Deck mDeck;
    /**
     * The current role of the user.  This value can change during the lifetime
     * of the activity.
     */
    private Role mRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        // Do this initialization early on in case it needs to start the AccountManager.
        DB.Singleton.get(getApplicationContext()).init(this);

        setContentView(R.layout.activity_presentation);

        if (savedInstanceState == null) {
            Log.d(TAG, "savedInstanceState is null");
            mDeck = DeckImpl.fromBundle(getIntent().getExtras());
            mRole = (Role) getIntent().getSerializableExtra(
                    Participant.B.PARTICIPANT_ROLE);
        } else {
            Log.d(TAG, "savedInstanceState is NOT null");
            mDeck = DeckImpl.fromBundle(savedInstanceState);
            mRole = (Role) savedInstanceState.get(Participant.B.PARTICIPANT_ROLE);
        }

        // TODO(jregan): This appears to be an attempt to avoid fragment
        // re-inflation, possibly the right thing to do is move the code
        // below to another flow step, e.g. onRestoreInstanceState.
        if (savedInstanceState != null) {
            return;
        }

        getSupportActionBar().setTitle(mDeck.getTitle());

        // If this is an audience member, we want them to jump straight to the fullscreen view.
        if (mRole == Role.AUDIENCE) {
            NavigateFragment fragment = NavigateFragment.newInstanceSynced(
                    mDeck.getId(), "randomPresentationId1", 0, mRole);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment, fragment)
                    .commit();
            showFullscreenSlide(0);
        } else {
            showSlideList();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle b) {
        Log.d(TAG, "onSaveInstanceState1");
        super.onSaveInstanceState(b);
        Log.d(TAG, "onSaveInstanceState2");
        packBundle(b);
    }

    private Bundle packBundle(Bundle b) {
        mDeck.toBundle(b);
        b.putSerializable(Participant.B.PARTICIPANT_ROLE, mRole);
        return b;
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        // Don't shutdown v23 at this point.
        // TODO(jregan): Stop advertising the live presentation if necessary.
    }

    /**
     * Set the system UI to be immersive or not.
     */
    public void setUiImmersive(boolean immersive) {
        if (immersive) {
            getSupportActionBar().hide();
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            getSupportActionBar().show();
            // See the comment at the top of fragment_slide_list.xml for why we don't simply
            // use View.SYSTEM_UI_FLAG_VISIBLE.
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    /**
     * Swap out the current fragment for a NavigateFragment. The presenter and
     * audience members who join live presentations will be synced.
     *
     * @param slideNum the slide to show
     */
    public void jumpToSlideSynced(int slideNum) {
        NavigateFragment fragment = NavigateFragment.newInstanceSynced(
                mDeck.getId(), "randomPresentationId1", slideNum, mRole);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, fragment)
                .commit();
    }

    /**
     * Swap out the current fragment for a NavigateFragment. Audience members
     * who click on a slide from the slide list will start unsynced.
     *
     * @param slideNum the slide to show
     */
    public void jumpToSlideUnsynced(int slideNum) {
        NavigateFragment fragment = NavigateFragment.newInstanceUnsynced(
                mDeck.getId(), "randomPresentationId1", slideNum, mRole);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, fragment)
                .commit();
    }

    private void beginAdvertising() {
        Log.d(TAG, "beginAdvertising");
        Intent intent = new Intent(this, ParticipantPeer.class);
        intent.putExtras(packBundle(new Bundle()));
        stopService(intent);
        startService(intent);
    }

    /**
     * Starts a live presentation.  The presentation will be advertised to other
     * devices as long as this activity is alive.
     */
    public void startPresentation() {
        DB db = DB.Singleton.get(getApplicationContext());
        db.createPresentation(mDeck.getId(), new DB.Callback<DB.CreatePresentationResult>() {
            @Override
            public void done(DB.CreatePresentationResult startPresentationResult) {
                Log.i(TAG, "Started presentation");
                Toast.makeText(getApplicationContext(), "Started presentation",
                        Toast.LENGTH_SHORT).show();
                if (Participant.ENABLE_MT_DISCOVERY) {
                    beginAdvertising();
                }
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
    public void showFullscreenSlide(int slideNum) {
        FullscreenSlideFragment fullscreenSlideFragment =
                FullscreenSlideFragment.newInstance(mDeck.getId(), "randomPresentationId1",
                        slideNum, mRole);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment, fullscreenSlideFragment)
                .addToBackStack("")
                .commit();
    }

    public void showSlideList() {
        SlideListFragment slideList = SlideListFragment.newInstance(mDeck.getId(), mRole);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment, slideList).commit();
    }
}
