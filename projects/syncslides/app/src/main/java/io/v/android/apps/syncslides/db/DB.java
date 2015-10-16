// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.db;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import java.util.List;

import io.v.android.apps.syncslides.model.Deck;
import io.v.android.apps.syncslides.model.Listener;
import io.v.android.apps.syncslides.model.Slide;

/**
 * Provides high-level methods for getting and setting the state of SyncSlides.
 * It is an interface instead of a concrete class to make testing easier.
 */
public interface DB {
    class Singleton {
        private static volatile DB instance;
        public static DB get(Context context) {
            DB result = instance;
            if (instance == null) {
                synchronized (Singleton.class) {
                    result = instance;
                    if (result == null) {
                        // Switch between FakeDB and SyncbaseDB by commenting out one.
                        instance = result = new FakeDB(context);
                        //instance = result = new SyncbaseDB(context);
                    }
                }
            }
            return result;
        }
    }

    /**
     * Perform initialization steps.  This method must be called early in the lifetime
     * of the activity.  As part of the initialization, it might send an intent to
     * another activity.
     *
     * @param activity implements onActivityResult() to call into DB.onActivityResult.
     */
    void init(Activity activity);

    /**
     * If init() sent an intent to another Activity, the result must be forwarded
     * from our app's activity to this method.
     *
     * @return true if the requestCode matches an intent sent by this implementation.
     */
    boolean onActivityResult(int requestCode, int resultCode, Intent data);

    /**
     * Provides a list of elements via an API that fits well with RecyclerView.Adapter.
     */
    interface DBList<E> {

        /**
         * Returns the number of items in the list.
         */
        int getItemCount();

        /**
         * Returns the ith item in the list.
         */
        E get(int i);

        /**
         * Sets the listener for changes to the list.  There can only be one listener.
         */
        void setListener(Listener listener);

        /**
         * Indicates that the list is no longer needed and should stop notifying its listener.
         */
        void discard();
    }

    /**
     * Add user to presenter's question queue.
     *
     * @param identity the user's identity name
     */
    void askQuestion(String identity);

    /**
     * Fetch the list of identities asking questions for the given deck.
     *
     * @param deckId   the deck to fetch
     * @param callback runs on the UI thread when the slide data is loaded
     */
    void getQuestionerList(String deckId, QuestionerListener callback);

    /** Listener for changes in the Q&A queue.
     *
     */
    interface QuestionerListener {
        /**
         * This callback is run on the UI thread to detect changes in the number of questions asked.
         *
         * @param questionerList the list of identities in the questions queue
         */
        void onChange(String[] questionerList);
    }

    /**
     * Gets the list of decks visible to the user.
     *
     * @return a list of decks
     */
    DBList<Deck> getDecks();

    /**
     * Given a deck ID, gets the list of slides visible to the user.
     *
     * @return a list of slides
     */
    DBList<Slide> getSlides(String deckId);

    interface Callback<Result> {
        /**
         * This callback is run on the UI thread once the method is done.
         *
         * @param result the loaded data
         */
        void done(Result result);
    }

    /**
     * Asynchronously fetch the slides for the given deck.
     *
     * @param deckId the deck to fetch
     * @param callback runs on the UI thread when the slide data is loaded
     */
    void getSlides(String deckId, Callback<List<Slide>> callback);

    /**
     * Imports the slide deck along with its slides.
     *
     * @param deck   deck to import
     * @param slides slides belonging to the above deck
     */
    void importDeck(Deck deck, Slide[] slides);

    /**
     * Asynchronously imports the slide deck along with its slides.
     *
     * @param deck     deck to import
     * @param slides   slides belonging to the above deck
     * @param callback runs on the UI thread when the deck has been imported
     */
    void importDeck(Deck deck, Slide[] slides, Callback<Void> callback);

    /**
     * Asynchronously deletes the deck and all of its slides.
     *
     * @param deckId  id of the deck to delete
     */
    void deleteDeck(String deckId);

    /**
     * Asynchronously deletes the deck and all of its slides.
     *
     * @param deckId    id of the deck to delete
     * @param callback  runs on the UI thread when the deck has been deleted
     */
    void deleteDeck(String deckId, Callback<Void> callback);

    class CreatePresentationResult {
        /**
         * A unique ID for the presentation.  All methods that deal with live presentation
         * data (e.g. the current slide) use this ID.
         */
        public String presentationId;
        /**
         * This is the name of the syncgroup that was created for this presentation instance.
         * Audience members must join this syncgroup.
         */
        public String syncgroupName;

        CreatePresentationResult(String presentationId, String syncgroupName) {
            this.presentationId = presentationId;
            this.syncgroupName = syncgroupName;
        }
    }

    /**
     * Creates a new presentation by creating a syncgroup.
     *
     * @param deckId the deck to use in the presentation
     * @param callback called when the presentation is created
     */
    void createPresentation(String deckId, Callback<CreatePresentationResult> callback);

    /**
     * Joins an existing presentation.
     *
     * @param syncgroupName the syncgroup to join
     * @param callback called when the syncgroup is joined
     */
    void joinPresentation(String syncgroupName, Callback<Void> callback);

    /**
     * Sets the current slide so any audience members can switch to it.
     *
     * @param deckId the deck being presented
     * @param presentationId the instance of the live presentation
     * @param slideNum the new slide number
     */
    void setCurrentSlide(String deckId, String presentationId, int slideNum);

    interface CurrentSlideListener {
        /**
         * Called whenever the current slide of a live presentation changes.
         *
         * @param slideNum the new slide number
         */
        void onChange(int slideNum);
    }

    /**
     * Add a listener for changes to the current slide of a live presentation.
     *
     * @param deckId the deck used in the presentation
     * @param presentationId the presentation to watch for changes
     * @param listener notified of changes
     */
    void addCurrentSlideListener(String deckId, String presentationId,
                                 CurrentSlideListener listener);

    /**
     * Remove a listener that was previously passed to addCurrentSlideListener().
     *
     * @param deckId the deck used in the presentation
     * @param presentationId the presentation being watched for changes
     * @param listener previously passed to addCurrentSlideListener()
     */
    void removeCurrentSlideListener(String deckId, String presentationId,
                                    CurrentSlideListener listener);
}
