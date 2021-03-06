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

import java.util.List;

import io.v.android.apps.syncslides.db.DB;
import io.v.android.apps.syncslides.model.Role;
import io.v.android.apps.syncslides.model.Slide;

public class FullscreenSlideFragment extends Fragment {

    private static final String DECK_ID_KEY = "deck_id";
    private static final String PRESENTATION_ID_KEY = "presentation_id";
    private static final String SLIDE_NUM_KEY = "slide_num";
    private static final String ROLE_KEY = "role";

    // TODO(afergan): Move state variables to activity.
    private String mDeckId;
    private String mPresentationId;
    private int mSlideNum;
    /**
     * While mSlides is loading, we can't validate any slide numbers coming from DB.
     * We hold them here until mSlides finishes loading.
     */
    private int mLoadingSlideNum;
    private Role mRole;
    private List<Slide> mSlides;
    private ImageView mFullScreenImage;
    private DB.CurrentSlideListener mCurrentSlideListener;

    public static FullscreenSlideFragment newInstance(String deckId, String presentationId,
                                                      int slideNum, Role role) {
        FullscreenSlideFragment fragment = new FullscreenSlideFragment();
        Bundle args = new Bundle();
        args.putString(DECK_ID_KEY, deckId);
        args.putString(PRESENTATION_ID_KEY, presentationId);
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
        mPresentationId = args.getString(PRESENTATION_ID_KEY);
        mSlideNum = args.getInt(SLIDE_NUM_KEY);
        mLoadingSlideNum = -1;
        mRole = (Role) args.get(ROLE_KEY);

        DB db = DB.Singleton.get(getActivity().getApplicationContext());
        db.getSlides(mDeckId, new DB.Callback<List<Slide>>() {
            @Override
            public void done(List<Slide> slides) {
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
                ((PresentationActivity)getActivity()).showNavigateFragment(mSlideNum);
            }
        });
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mRole == Role.AUDIENCE && ((PresentationActivity) getActivity()).getSynced()) {
            mCurrentSlideListener = new DB.CurrentSlideListener() {
                @Override
                public void onChange(int slideNum) {
                    currentSlideChanged(slideNum);
                }
            };
            DB.Singleton.get(getActivity().getApplicationContext())
                    .addCurrentSlideListener(mDeckId, mPresentationId, mCurrentSlideListener);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mRole == Role.AUDIENCE) {
            DB.Singleton.get(getActivity().getApplicationContext())
                    .removeCurrentSlideListener(mDeckId, mPresentationId, mCurrentSlideListener);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(DECK_ID_KEY, mDeckId);
        outState.putString(PRESENTATION_ID_KEY, mPresentationId);
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
        if (slideNum < 0 || slideNum >= mSlides.size()) {
            ((PresentationActivity)getActivity()).showNavigateFragment(0);
        }
        mSlideNum = slideNum;
        mFullScreenImage.setImageBitmap(mSlides.get(mSlideNum).getImage());
    }
}
