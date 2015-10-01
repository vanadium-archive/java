// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class PresentationActivity extends AppCompatActivity {
    //TODO(afergan): replace this with data from Syncbase
    public static final Slide[] SLIDES;

    static {
        int[] slideDrawables = new int[]{R.drawable.slide1, R.drawable.slide2, R.drawable.slide3,
                R.drawable.slide4, R.drawable.slide5, R.drawable.slide6, R.drawable.slide7};
        SLIDES = new Slide[slideDrawables.length];
        for (int i = 0; i < slideDrawables.length; ++i) {
            SLIDES[i] = new Slide(slideDrawables[i], "Notes for slide " + (i + 1));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presentation);
        if (savedInstanceState == null) {
            SlideListFragment slideList = new SlideListFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.fragment, slideList).commit();
        }
    }

    public static int getSlideImageId(int i) {
        return SLIDES[i].getSlideDrawableId();
    }

    public static String getSlideNotes(int i) {
        return SLIDES[i].getSlideNotes();
    }

    public static int getSlidesLength() {
        return SLIDES.length;
    }

    //This class encompasses all information for each slide in the presentation, including the
    // actual slide image, slide notes, and other configurable fields such as permissions or
    // question lists.
    private static class Slide {

        final int mSlideDrawableId;
        String mSlideNotes;

        private Slide(int slideDrawableId, String slideNotes) {
            mSlideDrawableId = slideDrawableId;
            mSlideNotes = slideNotes;
        }

        private int getSlideDrawableId() {
            return mSlideDrawableId;
        }

        private String getSlideNotes() {
            return mSlideNotes;
        }
    }
}
