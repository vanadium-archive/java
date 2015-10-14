// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import io.v.android.apps.syncslides.db.DB;
import io.v.android.apps.syncslides.model.Slide;

public class FullscreenSlideFragment extends Fragment {

    private static final String DECK_ID_KEY = "deck_id";
    private static final String SLIDE_NUM_KEY = "slide_num";
    private static final String ROLE_KEY = "role";

    private String mDeckId;
    private int mSlideNum;
    /**
     * While mSlides is loading, we can't validate any slide numbers coming from DB.
     * We hold them here until mSlides finishes loading.
     */
    private int mLoadingSlideNum;
    private Role mRole;
    private Slide[] mSlides;
    private ImageView mFullScreenImage;
    private DB.CurrentSlideListener mCurrentSlideListener;

    public static FullscreenSlideFragment newInstance(String deckId, int slideNum, Role role) {
        FullscreenSlideFragment fragment = new FullscreenSlideFragment();
        Bundle args = new Bundle();
        args.putString(DECK_ID_KEY, deckId);
        args.putInt(SLIDE_NUM_KEY, slideNum);
        args.putSerializable(ROLE_KEY, role);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle args = savedInstanceState;
        if (args == null) {
            args = getArguments();
        }
        mDeckId = args.getString(DECK_ID_KEY);
        mSlideNum = args.getInt(SLIDE_NUM_KEY);
        mLoadingSlideNum = -1;
        mRole = (Role) args.get(ROLE_KEY);

        DB db = DB.Singleton.get(getActivity().getApplicationContext());
        db.getSlides(mDeckId, new DB.Callback<Slide[]>() {
            @Override
            public void done(Slide[] slides) {
                mSlides = slides;
                // The CurrentSlideListener could have been notified while we were waiting for
                // the slides to load.
                if (mLoadingSlideNum != -1) {
                    currentSlideChanged(mLoadingSlideNum);
                } else {
                    currentSlideChanged(mSlideNum);
                }
            }
        });

        // See comment at the top of fragment_slide_list.xml.
        ((PresentationActivity) getActivity()).setUiImmersive(true);
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_fullscreen_slide, container, false);
        mFullScreenImage = (ImageView) rootView.findViewById(R.id.fullscreen_slide_image);
        mFullScreenImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mRole == Role.AUDIENCE) {
            mCurrentSlideListener = new DB.CurrentSlideListener() {
                @Override
                public void onChange(int slideNum) {
                    FullscreenSlideFragment.this.currentSlideChanged(slideNum);
                }
            };
            DB.Singleton.get(getActivity().getApplicationContext())
                    .addCurrentSlideListener(mCurrentSlideListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mRole == Role.AUDIENCE) {
            DB.Singleton.get(getActivity().getApplicationContext())
                    .removeCurrentSlideListener(mCurrentSlideListener);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(DECK_ID_KEY, mDeckId);
        outState.putInt(SLIDE_NUM_KEY, mSlideNum);
        outState.putSerializable(ROLE_KEY, mRole);
    }

    private void currentSlideChanged(int slideNum) {
        if (mSlides == null) {
            // We can't validate that slideNum is within the bounds of mSlides.  Hold it off
            // to the side until mSlides finishes loading.
            mLoadingSlideNum = slideNum;
            return;
        }
        if (slideNum < 0 || slideNum > mSlides.length) {
            getFragmentManager().popBackStack();
            return;
        }
        mSlideNum = slideNum;
        mFullScreenImage.setImageBitmap(mSlides[mSlideNum].getImage());
    }
}
