// Copyright 2015 The Vanadium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style
// license that can be found in the LICENSE file.

package io.v.android.apps.syncslides.discovery;

import android.graphics.Bitmap;

import io.v.android.apps.syncslides.model.Listener;

public interface Discovery {

        interface DList {
            /**
             * Returns the number of items in the list.
             */
            int getItemCount();

            /**
             * Returns the ith item in the list.
             */
            LivePresentation get(int i);

            /**
             * Sets the listener for changes to the list.  There can only be one listener.
             */
            void setListener(Listener listener);

            /**
             * Indicates that the list is no longer needed and should stop notifying its listener.
             */
            void discard();
        }

        interface LivePresentation {
            Bitmap getThumb();
            String getTitle();
            // TODO(afergan): getSyncgroup
        }

        DList getLivePresentations();

        void startLivePresentation(
                String deckId, String presentationId, String title, Bitmap thumb);

        void stopLivePresentation(String deckId, String presentationId);
}
