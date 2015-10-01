// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A fake implementation of DB for manual testing purposes.
 */
public class FakeDB implements DB {
    private static final int[] SLIDEDRAWABLES = new int[]{R.drawable.slide1, R.drawable.slide2,
            R.drawable.slide3, R.drawable.slide4, R.drawable.slide5, R.drawable.slide6,
            R.drawable.slide7};
    private final String[] SLIDENOTES = {"slide 1 notes", "slide 2 notes", "slide 3 notes",
            "slide 4 notes", "slide 5 notes", "slide 6 notes", "slide 7 notes"};
    private static final String TAG = "FakeDB";
    private static final int[] THUMBS = {
            R.drawable.thumb_deck1,
            R.drawable.thumb_deck2,
            R.drawable.thumb_deck3
    };
    private static final String[] TITLES = {"deck 1", "deck 2", "deck 3"};

    private final Bitmap[] mThumbs;
    private final Bitmap[] mSlideImages;

    public FakeDB(Context context) {
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
        private int mItemToAdd;
        private List<Bitmap> mGrowingThumbs;
        private List<String> mGrowingTitles;
        private AsyncTask<Void, Void, Void> mTask;
        private boolean mDiscarded;

        private FakeDeckList(Bitmap[] thumbs, String[] titles) {
            mThumbs = thumbs;
            mTitles = titles;
            mItemToAdd = 0;
            mGrowingThumbs = new ArrayList<>(Arrays.asList(mThumbs));
            mGrowingTitles = new ArrayList<>(Arrays.asList(mTitles));
            mTask = newAsyncTask();
            mTask.execute();
        }

        @Override
        public int getItemCount() {
            return mGrowingThumbs.size();
        }

        @Override
        public Deck getDeck(int i) {
            return new FakeDeck(mGrowingThumbs.get(i), mGrowingTitles.get(i), String.valueOf(i));
        }

        @Override
        public void setListener(Listener listener) {
            assert mListener == null;
            mListener = listener;
        }

        @Override
        public void discard() {
            mDiscarded = true;
            mTask.cancel(true);
        }

        /**
         * Creates a new AsyncTask that sleeps for a short time and then adds a new
         * deck to the list.
         */
        private AsyncTask<Void, Void, Void> newAsyncTask() {
            return new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        Log.v(TAG, "sleeping " + FakeDeckList.this);
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        // Nothing to do.
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                    if (!mDiscarded) {
                        Log.v(TAG, "growing  " + FakeDeckList.this);
                        mGrowingThumbs.add(mThumbs[mItemToAdd]);
                        mGrowingTitles.add(mTitles[mItemToAdd]);
                        mItemToAdd = (mItemToAdd + 1) % mThumbs.length;
                        if (mListener != null) {
                            mListener.notifyItemInserted(mGrowingThumbs.size() - 1);
                        }
                        if (mItemToAdd < 50) {
                            mTask = newAsyncTask();
                            mTask.execute();
                        }
                    } else {
                        Log.v(TAG, "discarded " + FakeDeckList.this);
                    }
                }
            };
        }
    }

    private static class FakeSlideList implements SlideList {
        private final Bitmap[] mSlideImages;
        private final String[] mSlideNotes;

        private FakeSlideList (Bitmap[] slideImages, String[] slideNotes) {
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
    public DeckList getDecks() {
        return new FakeDeckList(mThumbs, TITLES);
    }

    @Override
    public SlideList getSlides(String deckId) {
        // Always return the same set of slides no matter which deck was requested.
        return new FakeSlideList(mSlideImages, SLIDENOTES);
    }
}
