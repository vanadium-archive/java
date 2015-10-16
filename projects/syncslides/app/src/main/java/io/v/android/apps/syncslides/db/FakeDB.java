// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.db;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.common.collect.Lists;

import java.util.List;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.v.android.apps.syncslides.R;
import io.v.android.apps.syncslides.model.Deck;
import io.v.android.apps.syncslides.model.DeckImpl;
import io.v.android.apps.syncslides.model.Listener;
import io.v.android.apps.syncslides.model.Slide;

/**
 * A fake implementation of DB for manual testing purposes.
 */
public class FakeDB implements DB {
    private static final int[] SLIDEDRAWABLES = new int[]{
            R.drawable.slide1_thumb,
            R.drawable.slide2_thumb,
            R.drawable.slide3_thumb,
            R.drawable.slide4_thumb,
            R.drawable.slide5_thumb,
            R.drawable.slide6_thumb,
            R.drawable.slide7_thumb,
            R.drawable.slide8_thumb,
            R.drawable.slide9_thumb,
            R.drawable.slide10_thumb,
            R.drawable.slide11_thumb
    };
    private final String[] SLIDENOTES = {
            "This is the teaser slide. It should be memorable and descriptive of what your " +
                    "company is trying to do", "",
            "The bigger the pain, the better",
            "How do you solve this problem? How is it better or different from existing solutions?",
            "Demo the product", "", "[REDACTED]",
            "They may have tractor traction, but we still have the competitive advantage",
            "I'm not a businessman. I'm a business, man", "There is no 'i' on this slide",
            "Sqrt(all evil)"};
    private static final String TAG = "FakeDB";
    private static final int[] DECKTHUMBS = {
            R.drawable.thumb_deck1,
            R.drawable.thumb_deck2,
            R.drawable.thumb_deck3
    };
    private static final String[] DECKTITLES = {"deck 1", "deck 2", "deck 3"};

    private final Handler mHandler;

    private final FakeDeckList mDecks = new FakeDeckList();
    private final Map<String, FakeSlideList> mSlides = new HashMap();

    private List<CurrentSlideListener> mCurrentSlideListeners;
    private Thread mCurrentSlideWatcher;

    public FakeDB(Context context) {
        Slide[] slides = new Slide[SLIDEDRAWABLES.length];
        for (int i = 0; i < slides.length; ++i) {
            slides[i] = new FakeSlide(
                    BitmapFactory.decodeResource(context.getResources(), SLIDEDRAWABLES[i]),
                    SLIDENOTES[i]);
        }
        mHandler = new Handler(Looper.getMainLooper());
        for (int i = 0; i < DECKTHUMBS.length; ++i) {
            mDecks.add(new DeckImpl(
                    DECKTITLES[i],
                    BitmapFactory.decodeResource(context.getResources(), DECKTHUMBS[i]),
                    String.valueOf(i)));
            mSlides.put(String.valueOf(i), new FakeSlideList(slides));
        }
        mCurrentSlideListeners = Lists.newArrayList();
        mCurrentSlideWatcher = new Thread(new Runnable() {
            @Override
            public void run() {
                watchCurrentSlide();
            }
        });
        mCurrentSlideWatcher.start();
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

    private static class FakeDeckList implements DBList<Deck> {
        private final List<Deck> mDecks = new ArrayList();
        private Listener mListener;

        @Override
        public int getItemCount() {
            return mDecks.size();
        }

        @Override
        public Deck get(int i) {
            return mDecks.get(i);
        }

        @Override
        public void setListener(Listener listener) {
            mListener = listener;
        }

        @Override
        public void discard() {
            // Nothing to do.
        }

        private void add(Deck deck) {
            mDecks.add(deck);
            if (mListener != null) {
                mListener.notifyItemInserted(mDecks.size() - 1);
            }
        }

        private void delete(String deckId) {
            for (int i = 0; i < mDecks.size(); ++i) {
                if (mDecks.get(i).getId().equals(deckId)) {
                    mDecks.remove(i);
                    if (mListener != null) {
                        mListener.notifyItemRemoved(i);
                    }
                    return;
                }
            }
        }
    }

    private static class FakeSlideList implements DBList<Slide> {
        private final List<Slide> mSlides;

        private FakeSlideList(Slide[] slides) {
            // Slides don't currently change, so store them in the ImmutableList.
            mSlides = ImmutableList.<Slide>copyOf(slides);
        }

        @Override
        public int getItemCount() {
            return mSlides.size();
        }

        @Override
        public Slide get(int i) {
            return mSlides.get(i);
        }

        private List<Slide> getSlides() {
            return mSlides;
        }

        @Override
        public void setListener(Listener listener) {
        }

        @Override
        public void discard() {
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
    public DBList<Deck> getDecks() {
        return mDecks;
    }

    @Override
    public DBList<Slide> getSlides(String deckId) {
        return mSlides.get(deckId);
    }

    public void createPresentation(String deckId, final Callback<CreatePresentationResult> callback) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.done(new CreatePresentationResult("fakePresentationId", "sgname"));
            }
        });
    }

    @Override
    public void addCurrentSlideListener(CurrentSlideListener listener) {
        mCurrentSlideListeners.add(listener);
        // TODO(kash): It would be better to fire off a notification of the current
        // slide right away.  That requires storing the current slide in some
        // place that is accessible to the UI thread.  Too much work for now.
    }

    @Override
    public void removeCurrentSlideListener(CurrentSlideListener listener) {
        mCurrentSlideListeners.remove(listener);
    }

    @Override
    public void getSlides(String deckId, final Callback<List<Slide>> callback) {
        FakeSlideList list = mSlides.get(deckId);
        final List<Slide> slides = list == null ? null : list.getSlides();
        // Run the callback asynchronously on the UI thread.
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.done(slides);
            }
        });
    }

    @Override
    public void importDeck(Deck deck, Slide[] slides) {
        mDecks.add(deck);
        mSlides.put(deck.getId(), new FakeSlideList(slides));
    }

    @Override
    public void importDeck(final Deck deck,
                           final Slide[] slides,
                           final Callback<Void> callback) {
        new Thread() {
            @Override
            public void run() {
                importDeck(deck, slides);
                if (callback != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.done(null);
                        }
                    });
                }
            }
        }.start();
    }

    @Override
    public void deleteDeck(String deckId) {
        mDecks.delete(deckId);
    }

    @Override
    public void deleteDeck(final String deckId, final Callback<Void> callback) {
        new Thread() {
            @Override
            public void run() {
                deleteDeck(deckId);
                if (callback != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.done(null);
                        }
                    });
                }
            }
        }.start();
    }

    private void watchCurrentSlide() {
        try {
            int currentSlide = 0;
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(5000);
                // TODO(spetrovic): This assumes all decks have the same # of slides.  Fix it.
                currentSlide = (currentSlide + 1) % SLIDEDRAWABLES.length;
                final int finalCurrentSlide = currentSlide;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (CurrentSlideListener listener : mCurrentSlideListeners) {
                            listener.onChange(finalCurrentSlide);
                        }
                    }
                });
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Current Slide Watcher interrupted " + e);
        }
    }
}
