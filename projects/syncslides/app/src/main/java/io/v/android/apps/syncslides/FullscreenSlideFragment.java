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

    private static final String DECK_ID = "deck_id";
    private static final String SLIDE_NUM = "slide_num";

    private String mDeckId;
    private int mSlideNum;
    private Slide[] mSlides;
    private ImageView mFullScreenImage;

    public static FullscreenSlideFragment newInstance(String deckId, int slideNum) {
        FullscreenSlideFragment fragment = new FullscreenSlideFragment();
        Bundle args = new Bundle();
        args.putString(DECK_ID, deckId);
        args.putInt(SLIDE_NUM, slideNum);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Bundle args = getArguments();
        mDeckId = args.getString(DECK_ID);
        mSlideNum = args.getInt(SLIDE_NUM);

        DB db = DB.Singleton.get(getActivity().getApplicationContext());
        db.getSlides(mDeckId, new DB.Callback<Slide[]>() {
            @Override
            public void done(Slide[] slides) {
                mSlides = slides;
                if (mSlideNum >= 0 && mSlideNum < mSlides.length) {
                    mFullScreenImage.setImageBitmap(mSlides[mSlideNum].getImage());
                } else {
                    getFragmentManager().popBackStack();
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
}
