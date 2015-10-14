// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import android.content.Context;
import android.graphics.Bitmap;

import io.v.android.apps.syncslides.model.Deck;
import io.v.android.apps.syncslides.model.Listener;

public interface Discovery {
    class Singleton {
        private static volatile Discovery instance;

        public static Discovery get(Context context) {
            Discovery result = instance;
            if (instance == null) {
                synchronized (Singleton.class) {
                    result = instance;
                    if (result == null) {
                        // Switch between FakeDiscovery and real Discovery by commenting out one.
                        instance = result = new FakeDiscovery(context);
                        // TODO(jregan): Name the real discovery service.
                        //instance = result = new Discovery(context);
                    }
                }
            }
            return result;
        }
    }

    interface DList {
        /**
         * Returns the number of items in the list.
         */
        int getItemCount();

        /**
         * Returns the ith item in the list.
         */
        Deck get(int i);

        /**
         * Sets the listener for changes to the list.  There can only be one listener.
         */
        void setListener(Listener listener);

        /**
         * Indicates that the list is no longer needed and should stop notifying its listener.
         */
        void discard();
    }

    interface LivePresentation extends Deck {
        // TODO(afergan): getSyncgroup
    }

    DList getLivePresentations();

    void startLivePresentation(
            String deckId, String presentationId, String title, Bitmap thumb);

    void stopLivePresentation(String deckId, String presentationId);
}
