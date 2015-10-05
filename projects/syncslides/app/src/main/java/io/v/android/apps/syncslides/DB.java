// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;

import java.util.List;

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
                        instance = result = new FakeDB(context);
                    }
                }
            }
            return result;
        }
    }

    interface Deck {
        /**
         * Returns a Bitmap suitable as a thumbnail of the deck (e.g. the title slide).
         */
        Bitmap getThumb();

        /**
         * Returns the title of the deck.
         */
        String getTitle();

        /**
         * Returns the deck id.
         */
        String getId();
    }

    interface Slide {
        /**
         * Returns a Bitmap of the slide image.
         */
        Bitmap getImage();

        /**
         * Returns the slide notes.
         */
        String getNotes();
    }

    /**
     * Provides a list of Decks via an API that fits well with RecyclerView.Adapter.
     */
    interface DeckList {

        /**
         * Returns the number of items in the list.
         */
        int getItemCount();

        /**
         * Returns the ith item in the list.
         */
        Deck getDeck(int i);

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
     * Provides a list of Slides via an API that fits well with RecyclerView.Adapter.
     */
    interface SlideList {
        /**
         * Returns the number of items in the list.
         */
        int getItemCount();

        /**
         * Returns the ith item in the list.
         */
        Slide getSlide(int i);

    }

    /**
     * Callbacks for when the dataset changes dynamically.
     */
    interface Listener {
        void notifyItemChanged(int position);
        void notifyItemInserted(int position);
        void notifyItemRemoved(int position);
    }

    /**
     * Gets the list of decks visible to the user.
     * @return a list of decks
     */
    DeckList getDecks();

    /**
     * Given a deck ID, gets the list of slides visible to the user.
     * @return a list of slides
     */
    SlideList getSlides(String deckId);

    interface SlidesCallback {
        /**
         * This callback is run on the UI thread once the list of slides is loaded from the DB.
         * @param slides the loaded slide data
         */
        void done(Slide[] slides);
    }

    /**
     * Asynchronously fetch the slides for the given deck.
     * @param deckId the deck to fetch
     * @param callback runs on the UI thread when the slide data is loaded
     */
    void getSlides(String deckId, SlidesCallback callback);
}
