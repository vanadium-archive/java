// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.db;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import org.joda.time.DateTime;
import org.joda.time.Period;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import io.v.android.apps.syncslides.R;
import io.v.android.apps.syncslides.model.Deck;
import io.v.android.apps.syncslides.model.DeckFactory;
import io.v.android.apps.syncslides.model.Listener;
import io.v.android.apps.syncslides.model.Question;
import io.v.android.apps.syncslides.model.Slide;
import io.v.android.apps.syncslides.model.SlideImpl;

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

    private final List<Question> mQuestions;
    private final List<QuestionListener> mQuestionListeners;
    private Thread mQuestionWatcher;

    public FakeDB(Context context) {
        Slide[] slides = new Slide[SLIDEDRAWABLES.length];
        for (int i = 0; i < slides.length; ++i) {
            byte[] slide = DeckFactory.imageDataFromResource(context, SLIDEDRAWABLES[i]);
            slides[i] = new SlideImpl(slide, slide, SLIDENOTES[i]);
        }
        mHandler = new Handler(Looper.getMainLooper());
        for (int i = 0; i < DECKTHUMBS.length; ++i) {
            mDecks.add(DeckFactory.Singleton.get(context).make(
                    DECKTITLES[i], DECKTHUMBS[i], i));
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

        mQuestions = Lists.newArrayList();
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            Question question = new Question(
                    "question" + i,
                    "Questioner #" + i,
                    DateTime.now().minus(Period.minutes(random.nextInt(5)))
                            .toInstant().getMillis());
            mQuestions.add(question);
        }
        mQuestionListeners = Lists.newArrayList();
        mQuestionWatcher = new Thread(new Runnable() {
            @Override
            public void run() {
                watchQuestions();
            }
        });
        mQuestionWatcher.start();
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
    public void init() {
        // Nothing to do.
    }

    @Override
    public void askQuestion(String deckId, String presentationId, String name) {
        // Nothing to do.
    }

    @Override
    public void handoffQuestion(String deckId, String presentationId, String questionId) {
        // Not implemented.
    }

    @Override
    public void resumeControl(String deckId, String presentationId) {
        // Not implemented.
    }

    @Override
    public void setDriverListener(String deckId, String presentationId, DriverListener listener) {
        // Not implemented.
    }

    @Override
    public void removeDriverListener(String deckId, String presentationId, DriverListener listener) {
        // Not implemented.
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
    public void joinPresentation(String syncgroupName, final Callback<Void> callback) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.done(null);
            }
        });
    }

    @Override
    public void setCurrentSlide(String deckId, String presentationId, int slideNum) {

    }

    @Override
    public void setSlideNotes(String deckId, int slideNum, String slideNotes) {

     }

    @Override
    public void addCurrentSlideListener(String deckId, String presentationId,
                                        CurrentSlideListener listener) {
        mCurrentSlideListeners.add(listener);
        // TODO(kash): It would be better to fire off a notification of the current
        // slide right away.  That requires storing the current slide in some
        // place that is accessible to the UI thread.  Too much work for now.
    }

    @Override
    public void removeCurrentSlideListener(String deckId, String presentationId,
                                           CurrentSlideListener listener) {
        mCurrentSlideListeners.remove(listener);
    }

    @Override
    public void setQuestionListener(String deckId, String presentationId, QuestionListener listener) {
        mQuestionListeners.add(listener);
    }

    @Override
    public void removeQuestionListener(String deckId, String presentationId, QuestionListener listener) {
        mQuestionListeners.remove(listener);
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
    public Deck getDeck(String deckId) {
        for (int i = 0; i < mDecks.getItemCount(); i++) {
            Deck result = mDecks.get(i);
            if (result.getId().equals(deckId)) {
                return result;
            }
        }
        return null;
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

    private void watchQuestions() {
        try {
            Random random = new Random();
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(5000);
                Collections.shuffle(mQuestions);
                int numQuestions = random.nextInt(mQuestions.size());
                final List<Question> questions = mQuestions.subList(0, numQuestions);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        for (QuestionListener listener : mQuestionListeners) {
                            listener.onChange(questions);
                        }
                    }
                });
            }
        } catch (InterruptedException e) {
            Log.e(TAG, "Question Watcher interrupted " + e);
        }
    }
}
