// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

/**
 * A fake implementation of DB for manual testing purposes.
 */
public class FakeDB implements DB {
    private static final int[] SLIDEDRAWABLES = new int[]{R.drawable.slide1, R.drawable.slide2,
            R.drawable.slide3, R.drawable.slide4, R.drawable.slide5, R.drawable.slide6,
            R.drawable.slide7};
    private final String[] SLIDENOTES = {"slide 1 notes", "slide 2 notes", "",
            "slide 4 notes", "", "slide 6 notes", "slide 7 notes"};
    private static final String TAG = "FakeDB";
    private static final int[] THUMBS = {
            R.drawable.thumb_deck1,
            R.drawable.thumb_deck2,
            R.drawable.thumb_deck3
    };
    private static final String[] TITLES = {"deck 1", "deck 2", "deck 3"};

    private final Handler mHandler;
    private final Bitmap[] mThumbs;
    private final Bitmap[] mSlideImages;

    public FakeDB(Context context) {
        mHandler = new Handler(Looper.getMainLooper());
        mThumbs = new Bitmap[THUMBS.length];
        for (int i = 0; i < THUMBS.length; i++) {
            mThumbs[i] = BitmapFactory.decodeResource(context.getResources(), THUMBS[i]);
        }
        mSlideImages = new Bitmap[SLIDEDRAWABLES.length];
        for (int i = 0; i < SLIDEDRAWABLES.length; i++) {
            mSlideImages[i] =
                    BitmapFactory.decodeResource(context.getResources(), SLIDEDRAWABLES[i]);
        }
    }

    private static class FakeDeck implements Deck {
        private final String mTitle;
        private final Bitmap mThumb;
        private final String mDeckId;

        FakeDeck(Bitmap thumb, String title, String deckId) {
            mThumb = thumb;
            mTitle = title;
            mDeckId = deckId;
        }

        @Override
        public Bitmap getThumb() {
            return mThumb;
        }

        @Override
        public String getTitle() {
            return mTitle;
        }

        @Override
        public String getId() {
            return mDeckId;
        }

    }

    private static class FakeSlide implements Slide {
        private final String mSlideNotes;
        private final Bitmap mSlideImage;

        FakeSlide(Bitmap slideImage, String slideNotes) {
            mSlideImage = slideImage;
            mSlideNotes = slideNotes;
        }

        @Override
        public Bitmap getImage() {
            return mSlideImage;
        }

        @Override
        public String getNotes() {
            return mSlideNotes;
        }
    }

    private static class FakeDeckList implements DeckList {
        private final Bitmap[] mThumbs;
        private final String[] mTitles;
        private Listener mListener;

        private FakeDeckList(Bitmap[] thumbs, String[] titles) {
            mThumbs = thumbs;
            mTitles = titles;
        }

        @Override
        public int getItemCount() {
            return mThumbs.length;
        }

        @Override
        public Deck getDeck(int i) {
            return new FakeDeck(mThumbs[i], mTitles[i], String.valueOf(i));
        }

        @Override
        public void setListener(Listener listener) {
            assert mListener == null;
            mListener = listener;
        }

        @Override
        public void discard() {
            // Nothing to do.
        }
    }

    private static class FakeSlideList implements SlideList {
        private final Bitmap[] mSlideImages;
        private final String[] mSlideNotes;

        private FakeSlideList(Bitmap[] slideImages, String[] slideNotes) {
            mSlideImages = slideImages;
            mSlideNotes = slideNotes;
        }

        @Override
        public int getItemCount() {
            return mSlideImages.length;
        }

        @Override
        public Slide getSlide(int i) {
            return new FakeSlide(mSlideImages[i], mSlideNotes[i]);
        }
    }


    @Override
    public void init(Activity activity) {
        // Nothing to do.
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        // Nothing to do.
        return false;
    }

    @Override
    public void askQuestion(String identity) {
        //TODO(afergan): send identity to syncbase
    }

    @Override
    public void getQuestionerList(String deckId, final QuestionerListener callback) {
        final String[] questionerList = new String[]{
                "Audience member #1", "Audience member #2", "Audience member #3"};
        // Run the callback asynchronously on the UI thread.
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onChange(questionerList);
            }
        });
    }

    @Override
    public DeckList getDecks() {
        return new FakeDeckList(mThumbs, TITLES);
    }

    @Override
    public SlideList getSlides(String deckId) {
        // Always return the same set of slides no matter which deck was requested.
        return new FakeSlideList(mSlideImages, SLIDENOTES);
    }

    @Override
    public void getSlides(String deckId, final SlidesCallback callback) {
        final Slide[] slides = new Slide[mSlideImages.length];
        for (int i = 0; i < mSlideImages.length; i++) {
            slides[i] = new FakeSlide(mSlideImages[i], SLIDENOTES[i]);
        }
        // Run the callback asynchronously on the UI thread.
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.done(slides);
            }
        });
    }
}